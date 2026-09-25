package com.minifb.auth.dto;

import com.minifb.auth.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotNull @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự") @MaxUtf8Bytes(72)
        String password,
    @NotBlank @Size(max = 50) String displayName) {

  public RegisterRequest {
    email = email == null ? null : email.trim();
  }

  @Override
  public String toString() {
    return "RegisterRequest[email=" + email + ", displayName=" + displayName + "]";
  }
}
