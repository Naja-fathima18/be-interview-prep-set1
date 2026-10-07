package com.example.beinterviewprep.shorturl.api;

import com.example.beinterviewprep.shorturl.service.ShortUrlService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {

  private final ShortUrlService shortUrlService;

  @GetMapping("/{code:[A-Za-z0-9]{1,8}}")
  public ResponseEntity<Void> redirect(@PathVariable String code) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(shortUrlService.visit(code)))
        .cacheControl(CacheControl.noStore())
        .build();
  }
}
