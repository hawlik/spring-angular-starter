package com.example.app.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.app.auth.dto.ChangePasswordRequest;
import com.example.app.auth.dto.ForgotPasswordRequest;
import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.dto.LoginResult;
import com.example.app.auth.dto.ResetPasswordRequest;
import com.example.app.auth.dto.UpdateProfileRequest;
import com.example.app.auth.dto.UserProfileResponse;
import com.example.app.auth.dto.VerifyResetTokenRequest;
import com.example.app.auth.model.RefreshToken;
import com.example.app.auth.security.AuthCookieFactory;
import com.example.app.auth.security.DeviceFingerprint;
import com.example.app.auth.security.DeviceInfo;
import com.example.app.auth.service.AuthService;
import com.example.app.auth.service.JwtService;
import com.example.app.auth.service.PasswordResetService;
import com.example.app.auth.service.RefreshTokenService;
import com.example.app.user.model.User;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;
  private final AuthCookieFactory cookieFactory;
  private final PasswordResetService passwordResetService;
  private final RefreshTokenService refreshTokenService;

  public AuthController(
      AuthService authService,
      JwtService jwtService,
      AuthCookieFactory cookieFactory,
      PasswordResetService passwordResetService,
      RefreshTokenService refreshTokenService
  ) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.cookieFactory = cookieFactory;
    this.passwordResetService = passwordResetService;
    this.refreshTokenService = refreshTokenService;
  }

  @PostMapping("/login")
  public ResponseEntity<Void> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest
  ) {
    try {
      LoginResult result = authService.login(request);
      DeviceInfo device = DeviceFingerprint.from(httpRequest);
      RefreshToken refreshToken = refreshTokenService.createForLogin(
          result.user(), request.isRememberMe(), device);
      Duration refreshTtl = Duration.between(Instant.now(), refreshToken.getExpiresAt());

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE,
              cookieFactory.accessToken(result.accessToken(), jwtService.getAccessTokenTtl()).toString())
          .header(HttpHeaders.SET_COOKIE,
              cookieFactory.refreshToken(refreshToken.getToken(), refreshTtl).toString())
          .build();
    } catch (AuthenticationException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = AuthCookieFactory.REFRESH_TOKEN_NAME, required = false) String refreshToken
  ) {
    if (refreshToken != null) {
      refreshTokenService.revokeByToken(refreshToken);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString())
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshToken().toString())
        .build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<Void> refresh(
      @CookieValue(name = AuthCookieFactory.REFRESH_TOKEN_NAME, required = false) String refreshToken
  ) {
    if (refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      RefreshTokenService.ValidateResult validated = refreshTokenService.validateAndRotate(refreshToken);
      User user = validated.user();
      RefreshToken newRefreshToken = refreshTokenService.createForRotation(
          user, validated.rememberMe(), validated.device());
      Duration refreshTtl = Duration.between(Instant.now(), newRefreshToken.getExpiresAt());

      List<String> roles = user.getRoles().stream()
          .map(r -> "ROLE_" + r.name())
          .toList();
      String accessToken = jwtService.createToken(user.getEmail(), roles);

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE,
              cookieFactory.accessToken(accessToken, jwtService.getAccessTokenTtl()).toString())
          .header(HttpHeaders.SET_COOKIE,
              cookieFactory.refreshToken(newRefreshToken.getToken(), refreshTtl).toString())
          .build();
    } catch (RefreshTokenService.InvalidRefreshTokenException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString())
          .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshToken().toString())
          .build();
    }
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Map<String, String>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request
  ) {
    passwordResetService.requestPasswordReset(request.email());
    return ResponseEntity.ok(
        Map.of("message", "If an account with that email exists, a reset link has been sent.")
    );
  }

  @PostMapping("/verify-reset-token")
  public ResponseEntity<Map<String, String>> verifyResetToken(
      @Valid @RequestBody VerifyResetTokenRequest request
  ) {
    try {
      PasswordResetService.VerifyResult result =
          passwordResetService.verifyResetToken(request.token());

      return ResponseEntity.ok(Map.of("token", result.token()));
    } catch (PasswordResetService.InvalidResetTokenException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Map<String, String>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request
  ) {
    try {
      passwordResetService.resetPassword(request.token(), request.newPassword());
      return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    } catch (PasswordResetService.InvalidResetTokenException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/change-password")
  public ResponseEntity<Map<String, String>> changePassword(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody ChangePasswordRequest request
  ) {
    try {
      authService.changePassword(principal.getUsername(), request.currentPassword(), request.newPassword());
      return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    } catch (BadCredentialsException e) {
      return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect."));
    }
  }

  @GetMapping("/me")
  public UserProfileResponse me(@AuthenticationPrincipal UserDetails principal) {
    return authService.getUserProfile(principal.getUsername());
  }

  @PatchMapping("/profile")
  public UserProfileResponse updateProfile(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody UpdateProfileRequest request
  ) {
    return authService.updateProfile(principal.getUsername(), request);
  }
}
