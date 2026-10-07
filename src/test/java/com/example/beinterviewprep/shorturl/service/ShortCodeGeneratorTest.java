package com.example.beinterviewprep.shorturl.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

  @Test
  void generatesUrlSafeCodesOfConfiguredLength() {
    ShortCodeGenerator generator = generatorWithLength(7);

    for (int i = 0; i < 1_000; i++) {
      assertThat(generator.generate()).hasSize(7).matches("[0-9A-Za-z]+");
    }
  }

  @Test
  void supportsMaximumLengthOfEight() {
    assertThat(generatorWithLength(8).generate()).hasSize(8);
  }

  @Test
  void generatesDistinctCodes() {
    ShortCodeGenerator generator = generatorWithLength(7);
    Set<String> codes = new HashSet<>();

    for (int i = 0; i < 10_000; i++) {
      codes.add(generator.generate());
    }

    assertThat(codes).hasSize(10_000);
  }

  private static ShortCodeGenerator generatorWithLength(int length) {
    return new ShortCodeGenerator(new ShortUrlProperties("http://localhost:8080", length));
  }
}
