package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.app.user.model.User;
import com.example.app.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers
class PasswordResetTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private UserRepository userRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  private String requestResetAndGetEmailToken() throws Exception {
    mockMvc.perform(post("/auth/forgot-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            { "email": "user@example.com" }
            """));

    User user = userRepository.findByEmail("user@example.com").orElseThrow();
    return user.getPasswordResetToken();
  }

  private MvcResult verifyToken(String emailToken) throws Exception {
    return mockMvc.perform(post("/auth/verify-reset-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "token": "%s" }
                """.formatted(emailToken)))
        .andReturn();
  }

  private String extractExchangeToken(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
  }

  private String extractCsrfToken(MvcResult result) {
    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrfCookie).isNotNull();
    return csrfCookie.getValue();
  }

  @Nested
  @DisplayName("POST /auth/forgot-password")
  class ForgotPasswordTests {

    @Test
    @DisplayName("should return 200 with valid email")
    void forgotPasswordWithValidEmail() throws Exception {
      mockMvc.perform(post("/auth/forgot-password")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "email": "user@example.com" }
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(
              "If an account with that email exists, a reset link has been sent."));

      // Verify token was stored
      User user = userRepository.findByEmail("user@example.com").orElseThrow();
      assertThat(user.getPasswordResetToken()).isNotNull();
      assertThat(user.getPasswordResetTokenExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("should return 200 with unknown email (no enumeration)")
    void forgotPasswordWithUnknownEmail() throws Exception {
      mockMvc.perform(post("/auth/forgot-password")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "email": "unknown@example.com" }
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value(
              "If an account with that email exists, a reset link has been sent."));
    }

    @Test
    @DisplayName("should return 400 with invalid email format")
    void forgotPasswordWithInvalidEmail() throws Exception {
      mockMvc.perform(post("/auth/forgot-password")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "email": "not-an-email" }
                  """))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /auth/verify-reset-token")
  class VerifyResetTokenTests {

    @Test
    @DisplayName("should return 200 with exchange token and XSRF cookie for valid token")
    void verifyValidToken() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      mockMvc.perform(post("/auth/verify-reset-token")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "token": "%s" }
                  """.formatted(emailToken)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("should return 400 for expired token")
    void verifyExpiredToken() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      // Manually expire the token
      User user = userRepository.findByEmail("user@example.com").orElseThrow();
      user.setPasswordResetTokenExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
      userRepository.save(user);

      mockMvc.perform(post("/auth/verify-reset-token")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "token": "%s" }
                  """.formatted(emailToken)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("Reset token has expired"));
    }

    @Test
    @DisplayName("should return 400 for invalid/unknown token")
    void verifyInvalidToken() throws Exception {
      mockMvc.perform(post("/auth/verify-reset-token")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "token": "invalid-token-value" }
                  """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("Invalid reset token"));
    }

    @Test
    @DisplayName("should allow multiple verifications (page refresh)")
    void verifyTwiceSucceeds() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      mockMvc.perform(post("/auth/verify-reset-token")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "token": "%s" }
                  """.formatted(emailToken)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty());

      mockMvc.perform(post("/auth/verify-reset-token")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "token": "%s" }
                  """.formatted(emailToken)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty());
    }
  }

  @Nested
  @DisplayName("POST /auth/reset-password")
  class ResetPasswordTests {

    @Test
    @DisplayName("should reset password with valid 2-step flow")
    void resetPasswordWithValidFlow() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      MvcResult verifyResult = verifyToken(emailToken);
      assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);

      String exchangeToken = extractExchangeToken(verifyResult);
      String csrfToken = extractCsrfToken(verifyResult);

      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-XSRF-TOKEN", csrfToken)
              .cookie(new Cookie("XSRF-TOKEN", csrfToken))
              .content("""
                  { "token": "%s", "newPassword": "newpassword123" }
                  """.formatted(exchangeToken)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Password has been reset successfully."));

      // Verify token was cleared
      User updatedUser = userRepository.findByEmail("user@example.com").orElseThrow();
      assertThat(updatedUser.getPasswordResetToken()).isNull();
      assertThat(updatedUser.getPasswordResetTokenExpiresAt()).isNull();
    }

    @Test
    @DisplayName("should return 403 without CSRF token")
    void resetPasswordWithoutCsrf() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      MvcResult verifyResult = verifyToken(emailToken);
      String exchangeToken = extractExchangeToken(verifyResult);

      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "token": "%s", "newPassword": "newpassword123" }
                  """.formatted(exchangeToken)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 400 when replaying exchange JWT after successful reset")
    void replayExchangeJwtAfterReset() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      MvcResult verifyResult = verifyToken(emailToken);
      String exchangeToken = extractExchangeToken(verifyResult);
      String csrfToken = extractCsrfToken(verifyResult);

      // First reset succeeds
      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-XSRF-TOKEN", csrfToken)
              .cookie(new Cookie("XSRF-TOKEN", csrfToken))
              .content("""
                  { "token": "%s", "newPassword": "newpassword123" }
                  """.formatted(exchangeToken)))
          .andExpect(status().isOk());

      // Replay with same exchange token fails (email token cleared, hash mismatch)
      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-XSRF-TOKEN", csrfToken)
              .cookie(new Cookie("XSRF-TOKEN", csrfToken))
              .content("""
                  { "token": "%s", "newPassword": "anotherpassword" }
                  """.formatted(exchangeToken)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("Invalid reset token"));
    }

    @Test
    @DisplayName("should return 400 when using exchange JWT from old forgot-password after new request")
    void exchangeJwtFromOldForgotPassword() throws Exception {
      // First forgot-password + verify
      String emailToken1 = requestResetAndGetEmailToken();
      MvcResult verifyResult1 = verifyToken(emailToken1);
      String exchangeToken1 = extractExchangeToken(verifyResult1);
      String csrfToken1 = extractCsrfToken(verifyResult1);

      // Second forgot-password generates a new email token
      requestResetAndGetEmailToken();

      // Try to reset with the old exchange token — hash mismatch
      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-XSRF-TOKEN", csrfToken1)
              .cookie(new Cookie("XSRF-TOKEN", csrfToken1))
              .content("""
                  { "token": "%s", "newPassword": "newpassword123" }
                  """.formatted(exchangeToken1)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("Invalid reset token"));
    }

    @Test
    @DisplayName("should return 400 with invalid exchange JWT")
    void resetPasswordWithInvalidExchangeToken() throws Exception {
      String emailToken = requestResetAndGetEmailToken();
      MvcResult verifyResult = verifyToken(emailToken);
      String csrfToken = extractCsrfToken(verifyResult);

      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-XSRF-TOKEN", csrfToken)
              .cookie(new Cookie("XSRF-TOKEN", csrfToken))
              .content("""
                  { "token": "not-a-valid-jwt", "newPassword": "newpassword123" }
                  """))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 with too-short password")
    void resetPasswordWithTooShortPassword() throws Exception {
      String emailToken = requestResetAndGetEmailToken();
      MvcResult verifyResult = verifyToken(emailToken);
      String csrfToken = extractCsrfToken(verifyResult);

      mockMvc.perform(post("/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-XSRF-TOKEN", csrfToken)
              .cookie(new Cookie("XSRF-TOKEN", csrfToken))
              .content("""
                  { "token": "some-token", "newPassword": "short" }
                  """))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should allow login with new password after reset")
    void loginWithNewPasswordAfterReset() throws Exception {
      String emailToken = requestResetAndGetEmailToken();

      MvcResult verifyResult = verifyToken(emailToken);
      String exchangeToken = extractExchangeToken(verifyResult);
      String csrfToken = extractCsrfToken(verifyResult);

      // Reset the password
      mockMvc.perform(post("/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-XSRF-TOKEN", csrfToken)
          .cookie(new Cookie("XSRF-TOKEN", csrfToken))
          .content("""
              { "token": "%s", "newPassword": "mynewpassword" }
              """.formatted(exchangeToken)));

      // Login with the new password
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "username": "user@example.com", "password": "mynewpassword" }
                  """))
          .andExpect(status().isOk())
          .andExpect(cookie().exists("ACCESS_TOKEN"));

      // Old password should no longer work
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  { "username": "user@example.com", "password": "user" }
                  """))
          .andExpect(status().isUnauthorized());
    }
  }
}
