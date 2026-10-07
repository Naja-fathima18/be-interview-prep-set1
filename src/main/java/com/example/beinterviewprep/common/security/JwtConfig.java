package com.example.beinterviewprep.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  public static final String ROLES_CLAIM = "roles";
  public static final MacAlgorithm SIGNING_ALGORITHM = MacAlgorithm.HS256;

  private static final String ROLE_PREFIX = "ROLE_";

  @Bean
  public JwtEncoder jwtEncoder(JwtProperties properties) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
  }

  @Bean
  public JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(signingKey(properties))
            .macAlgorithm(SIGNING_ALGORITHM)
            .build();
    JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
    timestampValidator.setClock(clock);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestampValidator, new JwtIssuerValidator(properties.issuer())));
    return decoder;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName(ROLES_CLAIM);
    authorities.setAuthorityPrefix(ROLE_PREFIX);
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
  }

  private static SecretKey signingKey(JwtProperties properties) {
    return new SecretKeySpec(
        properties.secret().getBytes(StandardCharsets.UTF_8), SIGNING_ALGORITHM.getName());
  }
}
