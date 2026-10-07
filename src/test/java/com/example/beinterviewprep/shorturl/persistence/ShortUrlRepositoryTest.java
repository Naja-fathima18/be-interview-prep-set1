package com.example.beinterviewprep.shorturl.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.beinterviewprep.TestcontainersConfiguration;
import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");
  private static final Instant EXPIRES_AT = Instant.parse("2026-12-31T23:59:59Z");

  @Autowired private ShortUrlRepository shortUrlRepository;

  @Test
  void persistsShortUrlWithZeroVisits() {
    shortUrlRepository.saveAndFlush(
        new ShortUrl("abc1234", "https://example.com/a", CREATED_AT, EXPIRES_AT));

    ShortUrl found = shortUrlRepository.findByCode("abc1234").orElseThrow();

    assertThat(found.getOriginalUrl()).isEqualTo("https://example.com/a");
    assertThat(found.getVisitCount()).isZero();
    assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(found.getExpiresAt()).isEqualTo(EXPIRES_AT);
  }

  @Test
  void findsNothingForUnknownCode() {
    assertThat(shortUrlRepository.findByCode("missing")).isEmpty();
    assertThat(shortUrlRepository.existsByCode("missing")).isFalse();
  }

  @Test
  void reportsExistingCode() {
    shortUrlRepository.saveAndFlush(shortUrl("abc1234"));

    assertThat(shortUrlRepository.existsByCode("abc1234")).isTrue();
  }

  @Test
  void rejectsDuplicateCode() {
    shortUrlRepository.saveAndFlush(shortUrl("abc1234"));

    assertThatThrownBy(() -> shortUrlRepository.saveAndFlush(shortUrl("abc1234")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void incrementsVisitCountInDatabase() {
    shortUrlRepository.saveAndFlush(shortUrl("abc1234"));

    int first = shortUrlRepository.incrementVisitCount("abc1234");
    int second = shortUrlRepository.incrementVisitCount("abc1234");

    assertThat(first).isEqualTo(1);
    assertThat(second).isEqualTo(1);
    assertThat(shortUrlRepository.findByCode("abc1234").orElseThrow().getVisitCount()).isEqualTo(2);
  }

  @Test
  void incrementsOnlyTheMatchingCode() {
    shortUrlRepository.saveAndFlush(shortUrl("abc1234"));
    shortUrlRepository.saveAndFlush(shortUrl("xyz9876"));

    shortUrlRepository.incrementVisitCount("abc1234");

    assertThat(shortUrlRepository.findByCode("xyz9876").orElseThrow().getVisitCount()).isZero();
  }

  @Test
  void updatesNoRowsForUnknownCode() {
    assertThat(shortUrlRepository.incrementVisitCount("missing")).isZero();
  }

  private static ShortUrl shortUrl(String code) {
    return new ShortUrl(code, "https://example.com/" + code, CREATED_AT, null);
  }
}
