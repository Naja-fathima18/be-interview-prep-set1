package com.example.beinterviewprep.shorturl.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import com.example.beinterviewprep.shorturl.service.ShortUrlProperties;
import com.example.beinterviewprep.shorturl.service.ShortUrlService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ShortUrlController.class)
@EnableConfigurationProperties(ShortUrlProperties.class)
class ShortUrlControllerTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShortUrlService shortUrlService;

  @Test
  void shortensUrlAndReturnsLocationOfStats() throws Exception {
    Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    when(shortUrlService.shorten("https://example.com/a", expiresAt))
        .thenReturn(new ShortUrl("abc1234", "https://example.com/a", CREATED_AT, expiresAt));

    mockMvc
        .perform(
            shorten(
                """
                {"url": "https://example.com/a", "expiresAt": "%s"}
                """
                    .formatted(expiresAt)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/short-urls/abc1234/stats"))
        .andExpect(jsonPath("$.code").value("abc1234"))
        .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
        .andExpect(jsonPath("$.originalUrl").value("https://example.com/a"))
        .andExpect(jsonPath("$.createdAt").value("2026-10-07T09:00:00Z"))
        .andExpect(jsonPath("$.expiresAt").value(expiresAt.toString()));
  }

  @Test
  void shortensUrlWithoutExpiry() throws Exception {
    when(shortUrlService.shorten(eq("https://example.com/a"), isNull()))
        .thenReturn(new ShortUrl("abc1234", "https://example.com/a", CREATED_AT, null));

    mockMvc
        .perform(shorten("{\"url\": \"https://example.com/a\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("abc1234"))
        .andExpect(jsonPath("$.expiresAt").doesNotExist());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ftp://example.com/file", "not a url", "example.com", "https://"})
  void rejectsInvalidUrl(String url) throws Exception {
    mockMvc
        .perform(shorten("{\"url\": \"%s\"}".formatted(url)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("url"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("URL must be an absolute http or https URL with a host"));
    verifyNoInteractions(shortUrlService);
  }

  @Test
  void rejectsMissingUrl() throws Exception {
    mockMvc
        .perform(shorten("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("url"))
        .andExpect(jsonPath("$.errors[0].message").value("URL is required"));
    verifyNoInteractions(shortUrlService);
  }

  @Test
  void rejectsUrlLongerThan2048Characters() throws Exception {
    String url = "https://example.com/" + "a".repeat(2049 - "https://example.com/".length());

    mockMvc
        .perform(shorten("{\"url\": \"%s\"}".formatted(url)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("url"))
        .andExpect(jsonPath("$.errors[0].message").value("URL must be at most 2048 characters"));
    verifyNoInteractions(shortUrlService);
  }

  @Test
  void acceptsUrlOfExactly2048Characters() throws Exception {
    String url = "https://example.com/" + "a".repeat(2048 - "https://example.com/".length());
    when(shortUrlService.shorten(eq(url), isNull()))
        .thenReturn(new ShortUrl("abc1234", url, CREATED_AT, null));

    mockMvc.perform(shorten("{\"url\": \"%s\"}".formatted(url))).andExpect(status().isCreated());
  }

  @Test
  void rejectsExpiryInThePast() throws Exception {
    mockMvc
        .perform(
            shorten(
                """
                {"url": "https://example.com/a", "expiresAt": "%s"}
                """
                    .formatted(Instant.now().minus(1, ChronoUnit.HOURS))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("expiresAt"))
        .andExpect(jsonPath("$.errors[0].message").value("Expiry must be in the future"));
    verifyNoInteractions(shortUrlService);
  }

  @Test
  void rejectsMalformedExpiry() throws Exception {
    mockMvc
        .perform(shorten("{\"url\": \"https://example.com/a\", \"expiresAt\": \"tomorrow\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("expiresAt"));
    verifyNoInteractions(shortUrlService);
  }

  @Test
  void returnsGenericProblemWhenNoUnusedCodeIsFound() throws Exception {
    when(shortUrlService.shorten(any(), any()))
        .thenThrow(new IllegalStateException("No unused short code found after 5 attempts"));

    mockMvc
        .perform(shorten("{\"url\": \"https://example.com/a\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
  }

  @Test
  void returnsStatsForCode() throws Exception {
    ShortUrl shortUrl =
        new ShortUrl(
            "abc1234", "https://example.com/a", CREATED_AT, Instant.parse("2026-12-31T23:59:59Z"));
    ReflectionTestUtils.setField(shortUrl, "visitCount", 42L);
    when(shortUrlService.get("abc1234")).thenReturn(shortUrl);

    mockMvc
        .perform(get("/api/short-urls/abc1234/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("abc1234"))
        .andExpect(jsonPath("$.originalUrl").value("https://example.com/a"))
        .andExpect(jsonPath("$.visitCount").value(42))
        .andExpect(jsonPath("$.createdAt").value("2026-10-07T09:00:00Z"))
        .andExpect(jsonPath("$.expiresAt").value("2026-12-31T23:59:59Z"));
  }

  @Test
  void returnsNotFoundForStatsOfUnknownCode() throws Exception {
    when(shortUrlService.get("missing"))
        .thenThrow(new ResourceNotFoundException("Short URL", "missing"));

    mockMvc
        .perform(get("/api/short-urls/missing/stats"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Short URL with id missing was not found"));
  }

  private static MockHttpServletRequestBuilder shorten(String body) {
    return post("/api/short-urls").contentType(MediaType.APPLICATION_JSON).content(body);
  }
}
