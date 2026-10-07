package com.example.beinterviewprep.shorturl.service;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

  static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private final RandomGenerator random;
  private final int length;

  @Autowired
  public ShortCodeGenerator(ShortUrlProperties properties) {
    this(properties, new SecureRandom());
  }

  ShortCodeGenerator(ShortUrlProperties properties, RandomGenerator random) {
    this.length = properties.codeLength();
    this.random = random;
  }

  public String generate() {
    StringBuilder code = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
