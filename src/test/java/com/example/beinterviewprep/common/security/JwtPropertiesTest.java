package com.example.beinterviewprep.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  private static final String VALID_SECRET = "a-signing-key-that-is-long-enough-1234";
  private static final Duration TTL = Duration.ofMinutes(15);

  @Test
  void acceptsSecretOfAtLeastThirtyTwoBytes() {
    JwtProperties properties = new JwtProperties("x".repeat(32), TTL, "issuer");

    assertThat(properties.ttl()).isEqualTo(TTL);
  }

  @Test
  void rejectsMissingSecret() {
    assertThatThrownBy(() -> new JwtProperties(null, TTL, "issuer"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JWT_SECRET");
  }

  @Test
  void rejectsBlankSecret() {
    assertThatThrownBy(() -> new JwtProperties("  ", TTL, "issuer"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsSecretShorterThanThirtyTwoBytes() {
    assertThatThrownBy(() -> new JwtProperties("x".repeat(31), TTL, "issuer"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 32 bytes");
  }

  @Test
  void rejectsNonPositiveTtl() {
    assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, Duration.ZERO, "issuer"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, null, "issuer"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingIssuer() {
    assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, TTL, ""))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
