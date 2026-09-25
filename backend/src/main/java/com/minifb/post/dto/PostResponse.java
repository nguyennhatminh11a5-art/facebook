package com.minifb.post.dto;

import com.minifb.reaction.ReactionType;
import com.minifb.user.dto.UserSummary;
import java.time.Instant;
import java.util.List;

public record PostResponse(
    long id,
    UserSummary author,
    String content,
    List<ImageResponse> images,
    int reactionCount,
    int commentCount,
    ReactionType myReaction,
    Instant createdAt,
    Instant updatedAt,
    boolean editable) {

  public PostResponse {
    images = List.copyOf(images);
  }

  /** URL ký chỉ được tạo cho người xem được bài. */
  public record ImageResponse(String url, String thumbUrl, int width, int height) {}
}
