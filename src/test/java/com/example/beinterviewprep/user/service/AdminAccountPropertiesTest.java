package com.example.beinterviewprep.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminAccountPropertiesTest {

  @Test
  void isNotConfiguredWhenEmailAndPasswordAreBlank() {
    assertThat(new AdminAccountProperties("", " ").isConfigured()).isFalse();
    assertThat(new AdminAccountProperties(null, null).isConfigured()).isFalse();
  }

  @Test
  void isConfiguredWhenEmailAndPasswordAreSet() {
    assertThat(new AdminAccountProperties("admin@example.com", "admin-password").isConfigured())
        .isTrue();
  }

  @Test
  void rejectsEmailWithoutPassword() {
    assertThatThrownBy(() -> new AdminAccountProperties("admin@example.com", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be set together");
  }

  @Test
  void rejectsPasswordWithoutEmail() {
    assertThatThrownBy(() -> new AdminAccountProperties(null, "admin-password"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsWeakOrOverlongPassword() {
    assertThatThrownBy(() -> new AdminAccountProperties("admin@example.com", "short"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AdminAccountProperties("admin@example.com", "x".repeat(73)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void keepsPasswordOutOfToString() {
    assertThat(new AdminAccountProperties("admin@example.com", "admin-password").toString())
        .doesNotContain("admin-password")
        .doesNotContain("admin@example.com");
  }
}
