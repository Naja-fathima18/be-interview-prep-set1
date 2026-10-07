package com.example.beinterviewprep.user.api;

import com.example.beinterviewprep.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,
    @NotEmpty(message = "Password is required")
        @MaxUtf8Bytes(value = 72, message = "Password must be at most 72 bytes")
        String password) {

  @Override
  public String toString() {
    return "LoginRequest[email=***, password=***]";
  }
}
