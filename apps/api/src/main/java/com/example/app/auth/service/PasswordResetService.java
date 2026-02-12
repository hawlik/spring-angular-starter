package com.example.app.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.app.user.model.AccountStatus;
import com.example.app.user.model.User;
import com.example.app.user.repository.UserRepository;

@Service
public class PasswordResetService {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final JwtService jwtService;
  private final Clock clock;
  private final Duration tokenTtl;

  public PasswordResetService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      EmailService emailService,
      JwtService jwtService,
      Clock clock,
      @Value("${security.password-reset-token-ttl:PT1H}") Duration tokenTtl
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
    this.jwtService = jwtService;
    this.clock = clock;
    this.tokenTtl = tokenTtl;
  }

  @Transactional
  public void requestPasswordReset(String email) {
    userRepository.findByEmail(email)
        .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
        .ifPresent(user -> {
          String token = UUID.randomUUID().toString();
          user.setPasswordResetToken(token);
          user.setPasswordResetTokenExpiresAt(Instant.now(clock).plus(tokenTtl));
          userRepository.save(user);
          try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
          } catch (MailException e) {
            log.error("Failed to send password reset email to {}", user.getEmail(), e);
          }
        });
  }

  @Transactional(readOnly = true)
  public VerifyResult verifyResetToken(String emailToken) {
    User user = userRepository.findByPasswordResetToken(emailToken)
        .orElseThrow(() -> new InvalidResetTokenException("Invalid reset token"));

    if (user.getPasswordResetTokenExpiresAt().isBefore(Instant.now(clock))) {
      throw new InvalidResetTokenException("Reset token has expired");
    }

    String hash = sha256Hex(emailToken);
    String exchangeToken = jwtService.createPasswordResetToken(user.getEmail(), hash);
    return new VerifyResult(exchangeToken);
  }

  @Transactional
  public void resetPassword(String exchangeJwt, String newPassword) {
    JwtService.PasswordResetClaims claims;
    try {
      claims = jwtService.parsePasswordResetToken(exchangeJwt);
    } catch (IllegalArgumentException e) {
      throw new InvalidResetTokenException(e.getMessage());
    }

    User user = userRepository.findByEmail(claims.email())
        .orElseThrow(() -> new InvalidResetTokenException("Invalid reset token"));

    String currentTokenHash = user.getPasswordResetToken() != null
        ? sha256Hex(user.getPasswordResetToken())
        : null;

    if (!claims.emailTokenHash().equals(currentTokenHash)) {
      throw new InvalidResetTokenException("Invalid reset token");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setPasswordResetToken(null);
    user.setPasswordResetTokenExpiresAt(null);
    userRepository.save(user);
  }

  static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  public record VerifyResult(String token) {}

  public static class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException(String message) {
      super(message);
    }
  }
}
