package com.example.beinterviewprep.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@RequiredArgsConstructor
class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  private final ProblemDetailResponseWriter writer;

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    writer.write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        "Forbidden",
        "You do not have permission to access this resource");
  }
}
