package com.example.app.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.app.auth.model.RefreshToken;
import com.example.app.auth.repository.RefreshTokenRepository;
import com.example.app.auth.security.DeviceInfo;
import com.example.app.user.model.User;

@Service
@Transactional
public class RefreshTokenService {

  private final RefreshTokenRepository repository;
  private final Duration ttl;
  private final Duration shortTtl;

  public RefreshTokenService(
      RefreshTokenRepository repository,
      @Value("${security.jwt.refresh-token-ttl}") Duration ttl,
      @Value("${security.jwt.short-refresh-token-ttl}") Duration shortTtl
  ) {
    this.repository = repository;
    this.ttl = ttl;
    this.shortTtl = shortTtl;
  }

  private Duration effectiveTtl(boolean rememberMe) {
    if (rememberMe) {
      return ttl;
    }
    return shortTtl;
  }

  public record ValidateResult(User user, boolean rememberMe, DeviceInfo device) {}

  public RefreshToken createForLogin(User user, boolean rememberMe, DeviceInfo device) {
    if (device.fingerprint() != null) {
      repository.revokeByUserIdAndFingerprint(user.getId(), device.fingerprint());
    }
    return persist(user, rememberMe, device);
  }

  public RefreshToken createForRotation(User user, boolean rememberMe, DeviceInfo device) {
    return persist(user, rememberMe, device);
  }

  private RefreshToken persist(User user, boolean rememberMe, DeviceInfo device) {
    Instant now = Instant.now();
    Duration effective = effectiveTtl(rememberMe);
    RefreshToken token = new RefreshToken();
    token.setToken(UUID.randomUUID().toString());
    token.setUser(user);
    token.setExpiresAt(now.plus(effective));
    token.setRevoked(false);
    token.setRememberMe(rememberMe);
    token.setCreatedAt(now);
    token.setDeviceFingerprint(device.fingerprint());
    token.setDeviceLabel(device.label());
    token.setDeviceSummary(device.summary());
    return repository.save(token);
  }

  public ValidateResult validateAndRotate(String tokenValue) {
    RefreshToken token = repository.findByTokenForUpdate(tokenValue)
        .orElseThrow(InvalidRefreshTokenException::new);

    if (token.isRevoked()) {
      repository.revokeAllByUserId(token.getUser().getId());
      throw new InvalidRefreshTokenException();
    }

    if (token.getExpiresAt().isBefore(Instant.now())) {
      throw new InvalidRefreshTokenException();
    }

    token.setRevoked(true);
    DeviceInfo device = new DeviceInfo(
        token.getDeviceFingerprint(),
        token.getDeviceLabel(),
        token.getDeviceSummary());
    return new ValidateResult(token.getUser(), token.isRememberMe(), device);
  }

  public void revokeByToken(String tokenValue) {
    repository.findByToken(tokenValue).ifPresent(t -> t.setRevoked(true));
  }

  public static class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
      super("Invalid or expired refresh token");
    }
  }
}
