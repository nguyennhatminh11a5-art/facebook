package com.minifb.friendship;

import com.minifb.common.BadRequestException;
import com.minifb.common.ConflictException;
import com.minifb.common.Cursor;
import com.minifb.common.NotFoundException;
import com.minifb.common.PageResponse;
import com.minifb.common.Times;
import com.minifb.friendship.dto.FriendRequestResponse;
import com.minifb.friendship.dto.FriendshipStatusResponse;
import com.minifb.friendship.dto.SuggestionResponse;
import com.minifb.user.User;
import com.minifb.user.UserRepository;
import com.minifb.user.dto.UserSummary;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Bạn bè: mọi thao tác dùng userId của người kia. Từ chối/huỷ lời mời/huỷ kết bạn = xoá dòng. */
@Service
public class FriendshipService {

  static final int SUGGESTION_LIMIT = 20;

  private final FriendshipRepository friendships;
  private final UserRepository users;
  private final TransactionTemplate tx;
  private final Clock clock;

  public FriendshipService(
      FriendshipRepository friendships,
      UserRepository users,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.friendships = friendships;
    this.users = users;
    this.tx = new TransactionTemplate(transactionManager);
    this.clock = clock;
  }

  /**
   * Gửi lời mời. Người kia đã gửi cho mình → coi như chấp nhận. Hai bên gửi đồng thời → một bên vi
   * phạm {@code uq_friend_pair}; thử lại trong transaction mới sẽ thấy dòng của bên kia.
   */
  public FriendshipStatusResponse sendRequest(long me, long otherId) {
    if (me == otherId) {
      throw new BadRequestException("Không thể kết bạn với chính mình");
    }
    try {
      return tx.execute(status -> sendRequestOnce(me, otherId));
    } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
      return tx.execute(status -> sendRequestOnce(me, otherId));
    }
  }

  private FriendshipStatusResponse sendRequestOnce(long me, long otherId) {
    if (!users.existsById(otherId)) {
      throw new NotFoundException("Không tìm thấy người dùng");
    }
    Optional<Friendship> existing = friendships.findBetween(me, otherId);
    if (existing.isEmpty()) {
      friendships.saveAndFlush(Friendship.request(me, otherId, Times.now(clock)));
      return new FriendshipStatusResponse(otherId, FriendStatus.OUTGOING);
    }
    Friendship f = existing.get();
    if (f.getStatus() == Friendship.Status.PENDING && f.getRequesterId() == otherId) {
      f.accept(Times.now(clock));
      return new FriendshipStatusResponse(otherId, FriendStatus.FRIENDS);
    }
    throw new ConflictException(
        f.getStatus() == Friendship.Status.ACCEPTED
            ? "Hai bạn đã là bạn bè"
            : "Bạn đã gửi lời mời");
  }

  /** Chấp nhận lời mời **từ** {@code fromUserId}. */
  @Transactional
  public FriendshipStatusResponse accept(long me, long fromUserId) {
    Friendship f = pendingFrom(me, fromUserId);
    f.accept(Times.now(clock));
    return new FriendshipStatusResponse(fromUserId, FriendStatus.FRIENDS);
  }

  /** Từ chối (lời mời đến) hoặc huỷ (lời mời đi). */
  @Transactional
  public void deleteRequest(long me, long otherId) {
    Friendship f =
        friendships
            .findBetween(me, otherId)
            .filter(x -> x.getStatus() == Friendship.Status.PENDING)
            .orElseThrow(() -> new NotFoundException("Không có lời mời kết bạn"));
    friendships.delete(f);
  }

  @Transactional
  public void unfriend(long me, long otherId) {
    Friendship f =
        friendships
            .findBetween(me, otherId)
            .filter(x -> x.getStatus() == Friendship.Status.ACCEPTED)
            .orElseThrow(() -> new NotFoundException("Hai bạn chưa là bạn bè"));
    friendships.delete(f);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummary> friends(long me, String cursor, Integer size) {
    int pageSize = Cursor.size(size);
    List<User> found =
        cursor == null
            ? friendships.friendsFirstPage(me, pageSize + 1)
            : friendships.friendsNextPage(me, Cursor.decodeId(cursor), pageSize + 1);
    return PageResponse.of(found, pageSize, u -> Cursor.encode(u.getId())).map(UserSummary::of);
  }

  @Transactional(readOnly = true)
  public List<FriendRequestResponse> requests(long me, String direction) {
    List<Friendship> list =
        switch (direction == null ? "incoming" : direction) {
          case "incoming" -> friendships.incoming(me);
          case "outgoing" -> friendships.outgoing(me);
          default -> throw new BadRequestException("direction phải là incoming hoặc outgoing");
        };
    Map<Long, User> others = usersById(list.stream().map(f -> f.otherThan(me)).toList());
    return list.stream()
        .map(
            f ->
                new FriendRequestResponse(
                    UserSummary.of(others.get(f.otherThan(me))), f.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SuggestionResponse> suggestions(long me) {
    List<FriendshipRepository.Suggestion> rows = friendships.suggestions(me, SUGGESTION_LIMIT);
    Map<Long, User> byId =
        usersById(rows.stream().map(FriendshipRepository.Suggestion::getUserId).toList());
    return rows.stream()
        .map(r -> new SuggestionResponse(UserSummary.of(byId.get(r.getUserId())), r.getMutual()))
        .toList();
  }

  private Friendship pendingFrom(long me, long fromUserId) {
    return friendships
        .findBetween(me, fromUserId)
        .filter(f -> f.getStatus() == Friendship.Status.PENDING && f.getRequesterId() == fromUserId)
        .orElseThrow(() -> new NotFoundException("Không có lời mời kết bạn"));
  }

  private Map<Long, User> usersById(List<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    return users.findByIdIn(ids).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
  }
}
