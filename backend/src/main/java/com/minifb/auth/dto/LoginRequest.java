package com.minifb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 255) String email, @NotBlank @Size(max = 200) String password) {

  public LoginRequest {
    email = email == null ? null : email.trim();
  }

  @Override
  public String toString() {
    return "LoginRequest[email=" + email + "]";
  }
}
