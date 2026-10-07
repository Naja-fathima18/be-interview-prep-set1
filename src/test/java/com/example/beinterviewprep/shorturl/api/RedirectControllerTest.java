package com.example.beinterviewprep.shorturl.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.common.error.ShortUrlExpiredException;
import com.example.beinterviewprep.shorturl.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShortUrlService shortUrlService;

  @Test
  void redirectsWithFoundAndDisablesCaching() throws Exception {
    when(shortUrlService.visit("abc1234")).thenReturn("https://example.com/a?q=1");

    mockMvc
        .perform(get("/abc1234"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://example.com/a?q=1"))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void returnsNotFoundForUnknownCode() throws Exception {
    when(shortUrlService.visit("missing"))
        .thenThrow(new ResourceNotFoundException("Short URL", "missing"));

    mockMvc
        .perform(get("/missing"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Short URL with id missing was not found"));
  }

  @Test
  void returnsGoneForExpiredCode() throws Exception {
    when(shortUrlService.visit("abc1234")).thenThrow(new ShortUrlExpiredException("abc1234"));

    mockMvc
        .perform(get("/abc1234"))
        .andExpect(status().isGone())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(jsonPath("$.title").value("Resource expired"))
        .andExpect(jsonPath("$.detail").value("Short URL abc1234 has expired"))
        .andExpect(jsonPath("$.instance").value("/abc1234"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/abcdefghi", "/abc-123", "/abc_123"})
  void doesNotTreatNonCodePathsAsShortCodes(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isNotFound());

    verifyNoInteractions(shortUrlService);
  }
}
