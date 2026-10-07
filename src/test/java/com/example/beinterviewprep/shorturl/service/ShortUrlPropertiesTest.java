package com.example.beinterviewprep.shorturl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShortUrlPropertiesTest {

  @Test
  void buildsShortUrlFromBaseUrlAndCode() {
    assertThat(new ShortUrlProperties("https://sho.rt", 7).shortUrlFor("abc1234"))
        .isEqualTo("https://sho.rt/abc1234");
  }

  @Test
  void ignoresTrailingSlashOnBaseUrl() {
    assertThat(new ShortUrlProperties("https://sho.rt/", 7).shortUrlFor("abc1234"))
        .isEqualTo("https://sho.rt/abc1234");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 9})
  void rejectsCodeLengthOutsideOneToEight(int codeLength) {
    assertThatThrownBy(() -> new ShortUrlProperties("https://sho.rt", codeLength))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("code-length");
  }

  @Test
  void rejectsBlankBaseUrl() {
    assertThatThrownBy(() -> new ShortUrlProperties(" ", 7))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("base-url");
  }
}
