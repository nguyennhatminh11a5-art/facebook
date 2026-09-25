package com.minifb.friendship.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SendFriendRequest(@NotNull @Positive Long userId) {}
