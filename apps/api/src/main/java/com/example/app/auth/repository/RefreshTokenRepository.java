package com.example.app.auth.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.app.auth.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT t FROM RefreshToken t
      WHERE t.token = :token
      """)
  Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

  @Modifying
  @Query("""
      UPDATE RefreshToken t
      SET t.revoked = true
      WHERE t.user.id = :userId
        AND t.revoked = false
      """)
  void revokeAllByUserId(@Param("userId") Long userId);

  @Modifying
  @Query("""
      UPDATE RefreshToken t
      SET t.revoked = true
      WHERE t.user.id = :userId
        AND t.deviceFingerprint = :fingerprint
        AND t.revoked = false
      """)
  void revokeByUserIdAndFingerprint(
      @Param("userId") Long userId,
      @Param("fingerprint") String fingerprint);

  @Modifying
  @Query("""
      DELETE FROM RefreshToken t
      WHERE t.expiresAt < :expiredBefore
         OR (t.revoked = true AND t.createdAt < :revokedBefore)
      """)
  int deleteStale(
      @Param("expiredBefore") Instant expiredBefore,
      @Param("revokedBefore") Instant revokedBefore);
}
