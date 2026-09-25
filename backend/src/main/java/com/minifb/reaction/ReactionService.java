package com.minifb.reaction;

import com.minifb.common.NotFoundException;
import com.minifb.common.Times;
import com.minifb.post.PostAccessPolicy;
import com.minifb.post.PostRepository;
import com.minifb.reaction.dto.ReactionSummary;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reaction + bộ đếm trong cùng transaction (mục 5.2). Luôn khoá dòng bài trước để tránh deadlock
 * khi nhiều người react cùng một bài.
 */
@Service
public class ReactionService {

  private final ReactionRepository reactions;
  private final PostRepository posts;
  private final PostAccessPolicy accessPolicy;
  private final Clock clock;

  public ReactionService(
      ReactionRepository reactions,
      PostRepository posts,
      PostAccessPolicy accessPolicy,
      Clock clock) {
    this.reactions = reactions;
    this.posts = posts;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  /** PUT idempotent: thêm mới thì tăng đếm, đã có thì chỉ đổi loại. */
  @Transactional
  public ReactionSummary react(long viewerId, long postId, ReactionType type) {
    lockViewable(viewerId, postId);
    int inserted = reactions.insertIgnore(postId, viewerId, type.name(), Times.now(clock));
    if (inserted == 1) {
      posts.addReactionCount(postId, 1);
    } else {
      // Không dựa vào số dòng trả về: đặt cùng loại cũ có thể trả 0 tuỳ useAffectedRows
      reactions.updateType(postId, viewerId, type.name());
    }
    return new ReactionSummary(posts.reactionCount(postId), type);
  }

  @Transactional
  public ReactionSummary unreact(long viewerId, long postId) {
    lockViewable(viewerId, postId);
    if (reactions.deleteMine(postId, viewerId) == 1) {
      posts.addReactionCount(postId, -1);
    }
    return new ReactionSummary(posts.reactionCount(postId), null);
  }

  private void lockViewable(long viewerId, long postId) {
    posts
        .findByIdForUpdate(postId)
        .filter(p -> accessPolicy.canView(viewerId, p))
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
  }
}
