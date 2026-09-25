package com.minifb.user.dto;

import com.minifb.friendship.FriendStatus;

/** Hồ sơ cơ bản — mọi người đã đăng nhập đều xem được (mục 2). Không có email. */
public record ProfileResponse(
    long id, String displayName, String bio, String avatarUrl, FriendStatus friendStatus) {}
