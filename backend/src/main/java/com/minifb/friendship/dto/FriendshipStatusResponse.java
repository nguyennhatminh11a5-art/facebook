package com.minifb.friendship.dto;

import com.minifb.friendship.FriendStatus;

/** Quan hệ sau thao tác, nhìn từ phía người gọi. */
public record FriendshipStatusResponse(long userId, FriendStatus friendStatus) {}
