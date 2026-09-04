package com.example.app.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.app.auth.model.RefreshToken;
import com.example.app.auth.repository.RefreshTokenRepository;
import com.example.app.user.model.User;
import com.example.app.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RefreshTokenCleanupJobTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private RefreshTokenCleanupJob job;
  @Autowired private RefreshTokenRepository tokenRepository;
  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  void clearTokens() {
    tokenRepository.deleteAll();
    user = userRepository.findByEmail("user@example.com").orElseThrow();
  }

  @Test
  @DisplayName("deletes tokens expired more than 7 days ago")
  void deletesLongExpiredTokens() {
    Long staleId = insertToken(Instant.now().minus(Duration.ofDays(10)), false);

    job.purgeStale();

    assertThat(tokenRepository.findById(staleId)).isEmpty();
  }

  @Test
  @DisplayName("keeps tokens expired within the last 7 days for forensic visibility")
  void keepsRecentlyExpiredTokens() {
    Long recentId = insertToken(Instant.now().minus(Duration.ofDays(2)), false);

    job.purgeStale();

    assertThat(tokenRepository.findById(recentId)).isPresent();
  }

  @Test
  @DisplayName("keeps non-expired tokens regardless of age")
  void keepsActiveTokens() {
    Long activeId = insertToken(Instant.now().plus(Duration.ofDays(1)), false);

    job.purgeStale();

    assertThat(tokenRepository.findById(activeId)).isPresent();
  }

  @Test
  @DisplayName("deletes revoked tokens older than 30 days")
  void deletesOldRevokedTokens() {
    RefreshToken old = newToken(Instant.now().plus(Duration.ofDays(1)), true);
    old.setCreatedAt(Instant.now().minus(Duration.ofDays(45)));
    Long id = tokenRepository.save(old).getId();

    job.purgeStale();

    assertThat(tokenRepository.findById(id)).isEmpty();
  }

  @Test
  @DisplayName("keeps revoked tokens younger than 30 days")
  void keepsRecentRevokedTokens() {
    RefreshToken recent = newToken(Instant.now().plus(Duration.ofDays(1)), true);
    recent.setCreatedAt(Instant.now().minus(Duration.ofDays(5)));
    Long id = tokenRepository.save(recent).getId();

    job.purgeStale();

    assertThat(tokenRepository.findById(id)).isPresent();
  }

  private Long insertToken(Instant expiresAt, boolean revoked) {
    return tokenRepository.save(newToken(expiresAt, revoked)).getId();
  }

  private RefreshToken newToken(Instant expiresAt, boolean revoked) {
    RefreshToken token = new RefreshToken();
    token.setToken(UUID.randomUUID().toString());
    token.setUser(user);
    token.setExpiresAt(expiresAt);
    token.setRevoked(revoked);
    token.setRememberMe(false);
    token.setCreatedAt(Instant.now());
    return token;
  }
}
