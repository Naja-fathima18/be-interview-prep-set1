package com.example.beinterviewprep.user.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.beinterviewprep.user.domain.Role;
import com.example.beinterviewprep.user.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");

  @Autowired private UserRepository userRepository;

  @Test
  void persistsUserWithAllFields() {
    User saved =
        userRepository.saveAndFlush(new User("alice@example.com", "hash", Role.ADMIN, CREATED_AT));

    User found = userRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getEmail()).isEqualTo("alice@example.com");
    assertThat(found.getPasswordHash()).isEqualTo("hash");
    assertThat(found.getRole()).isEqualTo(Role.ADMIN);
    assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void storesEmailLowerCasedSoLookupIsCaseInsensitive() {
    userRepository.saveAndFlush(new User(" Alice@Example.COM ", "hash", Role.USER, CREATED_AT));

    assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
    assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
  }

  @Test
  void reportsUnknownEmailAsAbsent() {
    assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
  }

  @Test
  void rejectsSecondUserWithSameEmail() {
    userRepository.saveAndFlush(new User("bob@example.com", "hash", Role.USER, CREATED_AT));

    assertThatThrownBy(
            () ->
                userRepository.saveAndFlush(
                    new User("BOB@example.com", "other", Role.USER, CREATED_AT)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
