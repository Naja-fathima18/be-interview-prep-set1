package com.example.beinterviewprep.shorturl.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateShortUrlRequest(
    @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL must be at most 2048 characters")
        @HttpUrl(message = "URL must be an absolute http or https URL with a host")
        String url,
    @Future(message = "Expiry must be in the future") Instant expiresAt) {}
