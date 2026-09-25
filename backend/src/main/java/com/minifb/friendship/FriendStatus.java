package com.minifb.friendship;

import java.util.Optional;

/** Quan hệ giữa người xem và một người khác, nhìn từ phía người xem. */
public enum FriendStatus {
  NONE,
  OUTGOING,
  INCOMING,
  FRIENDS,
  SELF;

  public static FriendStatus of(long viewerId, long otherId, Optional<Friendship> friendship) {
    if (viewerId == otherId) {
      return SELF;
    }
    return friendship
        .map(
            f -> {
              if (f.getStatus() == Friendship.Status.ACCEPTED) {
                return FRIENDS;
              }
              return f.getRequesterId() == viewerId ? OUTGOING : INCOMING;
            })
        .orElse(NONE);
  }
}
