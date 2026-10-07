package com.example.beinterviewprep.user.service;

import com.example.beinterviewprep.common.security.JwtConfig;
import com.example.beinterviewprep.common.security.JwtProperties;
import com.example.beinterviewprep.user.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;
  private final Clock clock;

  public AccessToken issue(Long userId, Role role) {
    Instant issuedAt = Instant.now(clock);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject(String.valueOf(userId))
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(jwtProperties.ttl()))
            .claim(JwtConfig.ROLES_CLAIM, List.of(role.name()))
            .build();
    JwsHeader header = JwsHeader.with(JwtConfig.SIGNING_ALGORITHM).build();
    String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new AccessToken(value, jwtProperties.ttl());
  }
}
