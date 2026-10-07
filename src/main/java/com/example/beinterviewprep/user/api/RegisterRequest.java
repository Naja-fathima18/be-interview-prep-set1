package com.example.beinterviewprep.user.api;

import com.example.beinterviewprep.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,
    @NotNull(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must be at most 72 bytes")
        String password) {

  @Override
  public String toString() {
    return "RegisterRequest[email=***, password=***]";
  }
}
