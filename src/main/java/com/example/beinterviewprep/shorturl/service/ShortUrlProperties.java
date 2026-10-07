package com.example.beinterviewprep.shorturl.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.short-url")
public record ShortUrlProperties(
    @DefaultValue("http://localhost:8080") String baseUrl, @DefaultValue("7") int codeLength) {

  public static final int MAX_CODE_LENGTH = 8;

  public ShortUrlProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("app.short-url.base-url must not be blank");
    }
    if (codeLength < 1 || codeLength > MAX_CODE_LENGTH) {
      throw new IllegalArgumentException(
          "app.short-url.code-length must be between 1 and %d".formatted(MAX_CODE_LENGTH));
    }
    baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public String shortUrlFor(String code) {
    return baseUrl + "/" + code;
  }
}
