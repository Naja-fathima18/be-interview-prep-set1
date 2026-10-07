package com.example.beinterviewprep.shorturl.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

  static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private final SecureRandom random = new SecureRandom();
  private final int length;

  public ShortCodeGenerator(ShortUrlProperties properties) {
    this.length = properties.codeLength();
  }

  public String generate() {
    StringBuilder code = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
