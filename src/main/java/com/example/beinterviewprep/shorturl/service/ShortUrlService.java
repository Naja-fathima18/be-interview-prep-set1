package com.example.beinterviewprep.shorturl.service;

import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import com.example.beinterviewprep.shorturl.persistence.ShortUrlRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShortUrlService {

  static final int MAX_CODE_ATTEMPTS = 5;

  private final ShortUrlRepository shortUrlRepository;
  private final ShortCodeGenerator shortCodeGenerator;
  private final Clock clock;

  @Transactional
  public ShortUrl shorten(String originalUrl, Instant expiresAt) {
    ShortUrl saved =
        shortUrlRepository.save(
            new ShortUrl(unusedCode(), originalUrl, Instant.now(clock), expiresAt));
    log.info("Created short URL {}", saved.getCode());
    return saved;
  }

  private String unusedCode() {
    for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
      String code = shortCodeGenerator.generate();
      if (!shortUrlRepository.existsByCode(code)) {
        return code;
      }
      log.warn("Short code collision on attempt {}", attempt);
    }
    throw new IllegalStateException(
        "No unused short code found after %d attempts".formatted(MAX_CODE_ATTEMPTS));
  }
}
