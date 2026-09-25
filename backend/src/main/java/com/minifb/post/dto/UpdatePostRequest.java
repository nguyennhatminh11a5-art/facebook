package com.minifb.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(@NotNull @Size(max = 5000) String content) {}
