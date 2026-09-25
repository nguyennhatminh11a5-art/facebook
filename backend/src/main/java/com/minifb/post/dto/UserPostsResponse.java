package com.minifb.post.dto;

import com.minifb.friendship.FriendStatus;
import java.util.List;

/**
 * Bài của một người; không xem được → {@code items} rỗng + {@code friendStatus} để UI giải thích.
 */
public record UserPostsResponse(
    List<PostResponse> items, String nextCursor, FriendStatus friendStatus) {

  public UserPostsResponse {
    items = List.copyOf(items);
  }
}
