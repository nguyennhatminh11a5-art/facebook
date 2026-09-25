package com.minifb.comment.dto;

import com.minifb.user.dto.UserSummary;
import java.time.Instant;

public record CommentResponse(
    long id,
    long postId,
    UserSummary author,
    String content,
    Instant createdAt,
    boolean deletable) {}
