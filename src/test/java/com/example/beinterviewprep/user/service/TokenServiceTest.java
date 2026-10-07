package com.example.beinterviewprep.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.beinterviewprep.common.security.JwtConfig;
import com.example.beinterviewprep.common.security.JwtProperties;
import com.example.beinterviewprep.user.domain.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenServiceTest {

  private static final Instant ISSUED_AT = Instant.parse("2026-10-07T09:00:00Z");
  private static final Duration TTL = Duration.ofMinutes(15);
  private static final JwtProperties PROPERTIES =
      new JwtProperties("test-only-jwt-signing-key-not-for-production-use", TTL, "test-issuer");

  private final JwtConfig jwtConfig = new JwtConfig();
  private final TokenService tokenService =
      new TokenService(jwtConfig.jwtEncoder(PROPERTIES), PROPERTIES, clockAt(ISSUED_AT));

  @Test
  void issuesTokenWithUserIdRoleAndFifteenMinuteExpiry() {
    AccessToken token = tokenService.issue(42L, Role.ADMIN);

    Jwt jwt = decoderAt(ISSUED_AT).decode(token.value());

    assertThat(token.expiresIn()).isEqualTo(TTL);
    assertThat(jwt.getSubject()).isEqualTo("42");
    assertThat(jwt.getClaimAsStringList(JwtConfig.ROLES_CLAIM)).containsExactly("ADMIN");
    assertThat(jwt.getIssuedAt()).isEqualTo(ISSUED_AT);
    assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED_AT.plus(TTL));
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
  }

  @Test
  void acceptsTokenUpToItsExpiryInstant() {
    AccessToken token = tokenService.issue(42L, Role.USER);

    assertThat(decoderAt(ISSUED_AT.plus(TTL)).decode(token.value()).getSubject()).isEqualTo("42");
  }

  @Test
  void rejectsTokenOneSecondAfterFifteenMinutes() {
    AccessToken token = tokenService.issue(42L, Role.USER);

    JwtDecoder decoderAfterExpiry = decoderAt(ISSUED_AT.plus(TTL).plusSeconds(1));

    assertThatThrownBy(() -> decoderAfterExpiry.decode(token.value()))
        .isInstanceOf(JwtException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void rejectsTamperedToken() {
    String token = tokenService.issue(42L, Role.USER).value();
    String[] parts = token.split("\\.");
    String tampered = parts[0] + "." + parts[1] + "." + new StringBuilder(parts[2]).reverse();

    assertThatThrownBy(() -> decoderAt(ISSUED_AT).decode(tampered))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsTokenSignedWithAnotherKey() {
    JwtProperties otherKey =
        new JwtProperties("another-signing-key-that-is-long-enough-xyz", TTL, "test-issuer");
    String forged =
        new TokenService(jwtConfig.jwtEncoder(otherKey), otherKey, clockAt(ISSUED_AT))
            .issue(1L, Role.ADMIN)
            .value();

    assertThatThrownBy(() -> decoderAt(ISSUED_AT).decode(forged)).isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsTokenFromAnotherIssuer() {
    JwtProperties otherIssuer = new JwtProperties(PROPERTIES.secret(), TTL, "someone-else");
    String token =
        new TokenService(jwtConfig.jwtEncoder(otherIssuer), otherIssuer, clockAt(ISSUED_AT))
            .issue(1L, Role.USER)
            .value();

    assertThatThrownBy(() -> decoderAt(ISSUED_AT).decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void mapsRolesClaimToSpringRoleAuthorities() {
    Jwt jwt = decoderAt(ISSUED_AT).decode(tokenService.issue(42L, Role.ADMIN).value());

    assertThat(jwtConfig.jwtAuthenticationConverter().convert(jwt).getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void hidesTokenValueFromToString() {
    assertThat(tokenService.issue(42L, Role.USER).toString()).doesNotContain(".");
  }

  private JwtDecoder decoderAt(Instant now) {
    return jwtConfig.jwtDecoder(PROPERTIES, clockAt(now));
  }

  private static Clock clockAt(Instant instant) {
    return Clock.fixed(instant, ZoneOffset.UTC);
  }
}
