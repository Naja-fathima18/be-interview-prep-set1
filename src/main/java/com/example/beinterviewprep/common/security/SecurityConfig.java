package com.example.beinterviewprep.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Import(JwtConfig.class)
public class SecurityConfig {

  static final String SHORT_LINK_REDIRECT = "/{code:[A-Za-z0-9]{1,8}}";

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectMapper objectMapper,
      JwtAuthenticationConverter jwtAuthenticationConverter)
      throws Exception {
    ProblemDetailResponseWriter writer = new ProblemDetailResponseWriter(objectMapper);
    ProblemDetailAuthenticationEntryPoint entryPoint =
        new ProblemDetailAuthenticationEntryPoint(writer);
    ProblemDetailAccessDeniedHandler accessDeniedHandler =
        new ProblemDetailAccessDeniedHandler(writer);
    return http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers("/api/auth/**", "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, SHORT_LINK_REDIRECT)
                    .permitAll()
                    .requestMatchers("/api/users")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
