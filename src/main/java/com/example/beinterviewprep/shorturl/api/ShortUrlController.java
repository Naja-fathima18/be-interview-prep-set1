package com.example.beinterviewprep.shorturl.api;

import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import com.example.beinterviewprep.shorturl.service.ShortUrlProperties;
import com.example.beinterviewprep.shorturl.service.ShortUrlService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/short-urls")
@RequiredArgsConstructor
public class ShortUrlController {

  private final ShortUrlService shortUrlService;
  private final ShortUrlProperties properties;

  @PostMapping
  public ResponseEntity<ShortUrlResponse> shorten(
      @Valid @RequestBody CreateShortUrlRequest request) {
    ShortUrl shortUrl = shortUrlService.shorten(request.url(), request.expiresAt());
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{code}/stats")
            .buildAndExpand(shortUrl.getCode())
            .toUri();
    return ResponseEntity.created(location)
        .body(ShortUrlResponse.from(shortUrl, properties.shortUrlFor(shortUrl.getCode())));
  }

  @GetMapping("/{code}/stats")
  public ShortUrlStatsResponse stats(@PathVariable String code) {
    return ShortUrlStatsResponse.from(shortUrlService.get(code));
  }
}
