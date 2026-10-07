package com.example.beinterviewprep.shorturl.api;

import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import java.time.Instant;

public record ShortUrlResponse(
    String code, String shortUrl, String originalUrl, Instant createdAt, Instant expiresAt) {

  public static ShortUrlResponse from(ShortUrl shortUrl, String link) {
    return new ShortUrlResponse(
        shortUrl.getCode(),
        link,
        shortUrl.getOriginalUrl(),
        shortUrl.getCreatedAt(),
        shortUrl.getExpiresAt());
  }
}
