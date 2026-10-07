package com.example.beinterviewprep.shorturl.api;

import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import java.time.Instant;

public record ShortUrlStatsResponse(
    String code, String originalUrl, long visitCount, Instant createdAt, Instant expiresAt) {

  public static ShortUrlStatsResponse from(ShortUrl shortUrl) {
    return new ShortUrlStatsResponse(
        shortUrl.getCode(),
        shortUrl.getOriginalUrl(),
        shortUrl.getVisitCount(),
        shortUrl.getCreatedAt(),
        shortUrl.getExpiresAt());
  }
}
