package com.example.beinterviewprep.shorturl.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpUrlValidatorTest {

  private final HttpUrlValidator validator = new HttpUrlValidator();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://example.com",
        "https://example.com/path?q=1#frag",
        "HTTPS://EXAMPLE.COM",
        "https://sub.example.co.uk:8443/a/b",
        "http://127.0.0.1:8080/health"
      })
  void acceptsAbsoluteHttpUrlsWithHost(String url) {
    assertThat(validator.isValid(url, null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ftp://example.com/file",
        "not a url",
        "example.com",
        "/relative/path",
        "https://",
        "http:///no-host",
        "mailto:someone@example.com",
        "javascript:alert(1)",
        "https://exa mple.com"
      })
  void rejectsNonHttpOrHostlessUrls(String url) {
    assertThat(validator.isValid(url, null)).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void leavesMissingValuesToNotBlank(String url) {
    assertThat(validator.isValid(url, null)).isTrue();
  }
}
