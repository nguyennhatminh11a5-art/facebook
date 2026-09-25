package com.minifb.friendship;

import com.minifb.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

  /** Bạn bè ACCEPTED của :me (UNION ALL để mỗi nhánh dùng index riêng). */
  String MY_FRIENDS =
      "SELECT user_high AS fid FROM friendships WHERE user_low = :me AND status = 'ACCEPTED'"
          + " UNION ALL"
          + " SELECT user_low AS fid FROM friendships WHERE user_high = :me AND status = 'ACCEPTED'";

  interface Suggestion {
    Long getUserId();

    Long getMutual();
  }

  @Query("SELECT f FROM Friendship f WHERE f.userLow = :low AND f.userHigh = :high")
  Optional<Friendship> findPair(@Param("low") long low, @Param("high") long high);

  default Optional<Friendship> findBetween(long a, long b) {
    return findPair(Math.min(a, b), Math.max(a, b));
  }

  @Query(
      value =
          "SELECT COUNT(*) FROM friendships"
              + " WHERE user_low = :low AND user_high = :high AND status = 'ACCEPTED'",
      nativeQuery = true)
  long acceptedCount(@Param("low") long low, @Param("high") long high);

  default boolean areFriends(long a, long b) {
    return acceptedCount(Math.min(a, b), Math.max(a, b)) > 0;
  }

  @Query(value = MY_FRIENDS, nativeQuery = true)
  List<Long> friendIds(@Param("me") long me);

  /** Danh sách bạn bè, cursor = id của người bạn (tăng dần). */
  @Query(
      value =
          "SELECT u.* FROM users u JOIN ("
              + MY_FRIENDS
              + ") f ON u.id = f.fid"
              + " ORDER BY u.id ASC LIMIT :limit",
      nativeQuery = true)
  List<User> friendsFirstPage(@Param("me") long me, @Param("limit") int limit);

  @Query(
      value =
          "SELECT u.* FROM users u JOIN ("
              + MY_FRIENDS
              + ") f ON u.id = f.fid"
              + " WHERE u.id > :cId ORDER BY u.id ASC LIMIT :limit",
      nativeQuery = true)
  List<User> friendsNextPage(
      @Param("me") long me, @Param("cId") long cursorId, @Param("limit") int limit);

  /** Lời mời đến: người kia là requester. */
  @Query(
      "SELECT f FROM Friendship f WHERE f.status = com.minifb.friendship.Friendship.Status.PENDING"
          + " AND (f.userLow = :me OR f.userHigh = :me) AND f.requesterId <> :me"
          + " ORDER BY f.createdAt DESC")
  List<Friendship> incoming(@Param("me") long me);

  @Query(
      "SELECT f FROM Friendship f WHERE f.status = com.minifb.friendship.Friendship.Status.PENDING"
          + " AND f.requesterId = :me ORDER BY f.createdAt DESC")
  List<Friendship> outgoing(@Param("me") long me);

  /** Bạn của bạn, chưa có quan hệ nào với tôi, sắp theo số bạn chung. */
  @Query(
      value =
          "WITH my_friends AS ("
              + MY_FRIENDS
              + "), fof AS ("
              + " SELECT f.user_high AS cid FROM friendships f JOIN my_friends m"
              + "   ON f.user_low = m.fid WHERE f.status = 'ACCEPTED'"
              + " UNION ALL"
              + " SELECT f.user_low AS cid FROM friendships f JOIN my_friends m"
              + "   ON f.user_high = m.fid WHERE f.status = 'ACCEPTED')"
              + " SELECT cid AS userId, COUNT(*) AS mutual FROM fof"
              + " WHERE cid <> :me AND NOT EXISTS (SELECT 1 FROM friendships x"
              + "   WHERE x.user_low = LEAST(cid, :me) AND x.user_high = GREATEST(cid, :me))"
              + " GROUP BY cid ORDER BY mutual DESC, cid ASC LIMIT :limit",
      nativeQuery = true)
  List<Suggestion> suggestions(@Param("me") long me, @Param("limit") int limit);
}
