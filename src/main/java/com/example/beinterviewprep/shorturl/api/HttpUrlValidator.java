package com.example.beinterviewprep.shorturl.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    try {
      URI uri = new URI(value);
      return hasAllowedScheme(uri) && hasHost(uri);
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private static boolean hasAllowedScheme(URI uri) {
    return uri.getScheme() != null
        && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT));
  }

  private static boolean hasHost(URI uri) {
    return uri.getHost() != null && !uri.getHost().isBlank();
  }
}
