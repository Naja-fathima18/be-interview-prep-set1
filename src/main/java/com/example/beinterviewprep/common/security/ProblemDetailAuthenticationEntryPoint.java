package com.example.beinterviewprep.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

@RequiredArgsConstructor
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final BearerTokenAuthenticationEntryPoint bearerChallenge =
      new BearerTokenAuthenticationEntryPoint();
  private final ProblemDetailResponseWriter writer;

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    bearerChallenge.commence(request, response, exception);
    writer.write(request, response, HttpStatus.UNAUTHORIZED, "Unauthorized", detailOf(exception));
  }

  private static String detailOf(AuthenticationException exception) {
    return exception instanceof OAuth2AuthenticationException
        ? "The access token is invalid or has expired"
        : "Authentication is required to access this resource";
  }
}
