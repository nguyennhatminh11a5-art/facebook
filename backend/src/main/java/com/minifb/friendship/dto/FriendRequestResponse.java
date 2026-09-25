package com.minifb.friendship.dto;

import com.minifb.user.dto.UserSummary;
import java.time.Instant;

/**
 * Lời mời kết bạn; {@code user} là người kia (người gửi với lời mời đến, người nhận với lời mời
 * đi).
 */
public record FriendRequestResponse(UserSummary user, Instant createdAt) {}
