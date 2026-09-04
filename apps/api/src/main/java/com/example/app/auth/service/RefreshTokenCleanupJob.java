package com.example.app.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.app.auth.repository.RefreshTokenRepository;

@Component
public class RefreshTokenCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
  private static final Duration EXPIRED_GRACE = Duration.ofDays(7);
  private static final Duration REVOKED_GRACE = Duration.ofDays(30);

  private final RefreshTokenRepository repository;

  public RefreshTokenCleanupJob(RefreshTokenRepository repository) {
    this.repository = repository;
  }

  @Scheduled(cron = "${security.refresh-token.cleanup-cron:0 15 3 * * *}")
  @Transactional
  public void purgeStale() {
    try {
      Instant now = Instant.now();
      int deleted = repository.deleteStale(
          now.minus(EXPIRED_GRACE),
          now.minus(REVOKED_GRACE));
      log.info("Refresh token cleanup completed: purged {} rows", deleted);
    } catch (RuntimeException e) {
      log.error("Refresh token cleanup failed", e);
      throw e;
    }
  }
}
