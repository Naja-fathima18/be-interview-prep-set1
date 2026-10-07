package com.example.beinterviewprep.common.error;

public class ShortUrlExpiredException extends RuntimeException {

  public ShortUrlExpiredException(String code) {
    super("Short URL %s has expired".formatted(code));
  }
}
