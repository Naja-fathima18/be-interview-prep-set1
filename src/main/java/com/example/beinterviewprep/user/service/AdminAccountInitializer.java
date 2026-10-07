package com.example.beinterviewprep.user.service;

import com.example.beinterviewprep.common.error.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

  private final AdminAccountProperties properties;
  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isConfigured()) {
      log.info("No admin account configured; set ADMIN_EMAIL and ADMIN_PASSWORD to create one");
      return;
    }
    try {
      userService.createAdminIfAbsent(properties.email(), properties.password());
    } catch (ConflictException ex) {
      log.info("Admin account was created concurrently by another instance");
    }
  }
}
