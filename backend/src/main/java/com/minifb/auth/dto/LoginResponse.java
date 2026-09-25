package com.minifb.auth.dto;

import com.minifb.user.dto.MeResponse;

public record LoginResponse(String accessToken, MeResponse user) {}
