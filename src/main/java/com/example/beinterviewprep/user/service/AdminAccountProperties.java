package com.example.beinterviewprep.user.service;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.admin")
public record AdminAccountProperties(String email, String password) {

  static final int MIN_PASSWORD_LENGTH = 8;
  static final int MAX_PASSWORD_BYTES = 72;

  public AdminAccountProperties {
    email = blankToNull(email);
    password = blankToNull(password);
    if ((email == null) != (password == null)) {
      throw new IllegalArgumentException(
          "app.security.admin.email and app.security.admin.password must be set together");
    }
    if (password != null
        && (password.length() < MIN_PASSWORD_LENGTH
            || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES)) {
      throw new IllegalArgumentException(
          "app.security.admin.password must be %d characters to %d bytes long"
              .formatted(MIN_PASSWORD_LENGTH, MAX_PASSWORD_BYTES));
    }
  }

  public boolean isConfigured() {
    return email != null;
  }

  @Override
  public String toString() {
    return "AdminAccountProperties[configured=" + isConfigured() + "]";
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
