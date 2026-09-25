package com.minifb.post;

import com.minifb.friendship.FriendshipRepository;
import org.springframework.stereotype.Component;

/**
 * Nguồn chuẩn duy nhất của quy tắc "xem được bài" (CLAUDE.md mục 2): tác giả + bạn bè ACCEPTED của
 * tác giả. Mọi service gọi lại chỗ này.
 */
@Component
public class PostAccessPolicy {

  private final FriendshipRepository friendships;

  public PostAccessPolicy(FriendshipRepository friendships) {
    this.friendships = friendships;
  }

  public boolean canView(long viewerId, Post post) {
    return canViewPostsOf(viewerId, post.getUserId());
  }

  /** Người xem có xem được bài của {@code authorId} không. */
  public boolean canViewPostsOf(long viewerId, long authorId) {
    return viewerId == authorId || friendships.areFriends(viewerId, authorId);
  }
}
