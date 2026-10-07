package com.example.beinterviewprep.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

  static final int MIN_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException(
          "app.security.jwt.secret must be set, e.g. via the JWT_SECRET environment variable");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalArgumentException(
          "app.security.jwt.secret must be at least %d bytes long".formatted(MIN_SECRET_BYTES));
    }
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("app.security.jwt.ttl must be a positive duration");
    }
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("app.security.jwt.issuer must be set");
    }
  }
}
