package com.minifb.reaction.dto;

import com.minifb.reaction.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactRequest(@NotNull ReactionType type) {}
