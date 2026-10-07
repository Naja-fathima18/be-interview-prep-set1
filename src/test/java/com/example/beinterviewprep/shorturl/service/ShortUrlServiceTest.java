package com.example.beinterviewprep.shorturl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.common.error.ShortUrlExpiredException;
import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import com.example.beinterviewprep.shorturl.persistence.ShortUrlRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final Instant EXPIRES_AT = Instant.parse("2026-12-31T23:59:59Z");

  @Mock private ShortUrlRepository shortUrlRepository;
  @Mock private ShortCodeGenerator shortCodeGenerator;

  private ShortUrlService shortUrlService;

  @BeforeEach
  void setUp() {
    shortUrlService =
        new ShortUrlService(
            shortUrlRepository, shortCodeGenerator, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shortensUrlWithGeneratedCodeAndCreatedDateFromClock() {
    when(shortCodeGenerator.generate()).thenReturn("abc1234");
    returnSavedEntity();

    ShortUrl shortUrl = shortUrlService.shorten("https://example.com/a", EXPIRES_AT);

    assertThat(shortUrl.getCode()).isEqualTo("abc1234");
    assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com/a");
    assertThat(shortUrl.getCreatedAt()).isEqualTo(NOW);
    assertThat(shortUrl.getExpiresAt()).isEqualTo(EXPIRES_AT);
    assertThat(shortUrl.getVisitCount()).isZero();
  }

  @Test
  void retriesWhenGeneratedCodeIsAlreadyTaken() {
    when(shortCodeGenerator.generate()).thenReturn("taken01", "taken02", "free001");
    when(shortUrlRepository.existsByCode("taken01")).thenReturn(true);
    when(shortUrlRepository.existsByCode("taken02")).thenReturn(true);
    returnSavedEntity();

    ShortUrl shortUrl = shortUrlService.shorten("https://example.com/a", null);

    assertThat(shortUrl.getCode()).isEqualTo("free001");
  }

  @Test
  void givesUpAfterBoundedNumberOfCollisions() {
    when(shortCodeGenerator.generate()).thenReturn("taken01");
    when(shortUrlRepository.existsByCode("taken01")).thenReturn(true);

    assertThatThrownBy(() -> shortUrlService.shorten("https://example.com/a", null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(String.valueOf(ShortUrlService.MAX_CODE_ATTEMPTS));
    verify(shortUrlRepository, never()).save(any(ShortUrl.class));
  }

  @Test
  void createsNewCodeEachTimeTheSameUrlIsShortened() {
    when(shortCodeGenerator.generate()).thenReturn("first01", "second2");
    returnSavedEntity();

    ShortUrl first = shortUrlService.shorten("https://example.com/a", null);
    ShortUrl second = shortUrlService.shorten("https://example.com/a", null);

    assertThat(first.getCode()).isNotEqualTo(second.getCode());
  }

  @Test
  void visitCountsAtomicallyAndReturnsOriginalUrl() {
    when(shortUrlRepository.findByCode("abc1234"))
        .thenReturn(Optional.of(shortUrlExpiringAt(NOW.plusSeconds(1))));

    String target = shortUrlService.visit("abc1234");

    assertThat(target).isEqualTo("https://example.com/a");
    verify(shortUrlRepository).incrementVisitCount("abc1234");
  }

  @Test
  void visitCountsLinksWithoutExpiry() {
    when(shortUrlRepository.findByCode("abc1234"))
        .thenReturn(Optional.of(shortUrlExpiringAt(null)));

    assertThat(shortUrlService.visit("abc1234")).isEqualTo("https://example.com/a");
    verify(shortUrlRepository).incrementVisitCount("abc1234");
  }

  @Test
  void visitThrowsNotFoundForUnknownCode() {
    when(shortUrlRepository.findByCode("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> shortUrlService.visit("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(shortUrlRepository, never()).incrementVisitCount(anyString());
  }

  @Test
  void visitRejectsExpiredLinkWithoutCounting() {
    when(shortUrlRepository.findByCode("abc1234"))
        .thenReturn(Optional.of(shortUrlExpiringAt(NOW.minusSeconds(1))));

    assertThatThrownBy(() -> shortUrlService.visit("abc1234"))
        .isInstanceOf(ShortUrlExpiredException.class)
        .hasMessage("Short URL abc1234 has expired");
    verify(shortUrlRepository, never()).incrementVisitCount(anyString());
  }

  @Test
  void visitTreatsLinkAsExpiredAtTheExactExpiryInstant() {
    when(shortUrlRepository.findByCode("abc1234")).thenReturn(Optional.of(shortUrlExpiringAt(NOW)));

    assertThatThrownBy(() -> shortUrlService.visit("abc1234"))
        .isInstanceOf(ShortUrlExpiredException.class);
    verify(shortUrlRepository, never()).incrementVisitCount(anyString());
  }

  private static ShortUrl shortUrlExpiringAt(Instant expiresAt) {
    return new ShortUrl("abc1234", "https://example.com/a", NOW.minusSeconds(60), expiresAt);
  }

  private void returnSavedEntity() {
    when(shortUrlRepository.save(any(ShortUrl.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }
}
