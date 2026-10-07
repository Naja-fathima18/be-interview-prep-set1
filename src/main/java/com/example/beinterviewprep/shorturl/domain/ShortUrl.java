package com.example.beinterviewprep.shorturl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "short_url")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8, updatable = false)
  private String code;

  @Column(name = "original_url", nullable = false, length = 2048, updatable = false)
  private String originalUrl;

  @Column(name = "visit_count", nullable = false)
  private long visitCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", updatable = false)
  private Instant expiresAt;

  public ShortUrl(String code, String originalUrl, Instant createdAt, Instant expiresAt) {
    this.code = code;
    this.originalUrl = originalUrl;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public boolean isExpiredAt(Instant now) {
    return expiresAt != null && !expiresAt.isAfter(now);
  }
}
