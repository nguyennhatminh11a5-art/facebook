package com.minifb.friendship.dto;

import com.minifb.user.dto.UserSummary;

public record SuggestionResponse(UserSummary user, long mutualFriends) {}
