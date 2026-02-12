package com.example.app.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import com.example.app.auth.dto.ForgotPasswordRequest;
import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.dto.LoginResult;
import com.example.app.auth.dto.ResetPasswordRequest;
import com.example.app.auth.dto.UserProfileResponse;
import com.example.app.auth.dto.VerifyResetTokenRequest;
import com.example.app.auth.security.AuthCookieFactory;
import com.example.app.auth.service.AuthService;
import com.example.app.auth.service.JwtService;
import com.example.app.auth.service.PasswordResetService;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;
  private final AuthCookieFactory cookieFactory;
  private final PasswordResetService passwordResetService;

  public AuthController(
      AuthService authService,
      JwtService jwtService,
      AuthCookieFactory cookieFactory,
      PasswordResetService passwordResetService
  ) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.cookieFactory = cookieFactory;
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/login")
  public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
    try {
      LoginResult result = authService.login(request);

      return ResponseEntity.ok()
          .header(
              HttpHeaders.SET_COOKIE,
              cookieFactory
                  .accessToken(result.accessToken(), jwtService.getAccessTokenTtl())
                  .toString()
          )
          .build();
    } catch (AuthenticationException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString())
        .build();
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

  @GetMapping("/me")
  public UserProfileResponse me(@AuthenticationPrincipal UserDetails principal) {
    return authService.getUserProfile(principal.getUsername());
  }
}
