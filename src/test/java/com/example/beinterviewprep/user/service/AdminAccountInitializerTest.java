package com.example.beinterviewprep.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

  @Mock private UserService userService;

  @Test
  void createsConfiguredAdminOnStartup() {
    initializer(new AdminAccountProperties("admin@example.com", "admin-password"))
        .run(new DefaultApplicationArguments());

    verify(userService).createAdminIfAbsent("admin@example.com", "admin-password");
  }

  @Test
  void doesNothingWhenNoAdminIsConfigured() {
    initializer(new AdminAccountProperties(null, null)).run(new DefaultApplicationArguments());

    verifyNoInteractions(userService);
  }

  @Test
  void keepsStartingWhenAnotherInstanceCreatedTheAdminFirst() {
    when(userService.createAdminIfAbsent("admin@example.com", "admin-password"))
        .thenThrow(new ConflictException("An account with this email already exists"));

    assertThatCode(
            () ->
                initializer(new AdminAccountProperties("admin@example.com", "admin-password"))
                    .run(new DefaultApplicationArguments()))
        .doesNotThrowAnyException();
  }

  private AdminAccountInitializer initializer(AdminAccountProperties properties) {
    return new AdminAccountInitializer(properties, userService);
  }
}
