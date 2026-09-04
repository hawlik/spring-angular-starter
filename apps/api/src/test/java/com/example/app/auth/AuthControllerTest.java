package com.example.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import jakarta.servlet.http.Cookie;
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
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers
class AuthControllerTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Nested
  @DisplayName("POST /auth/login")
  class LoginTests {

    @Test
    @DisplayName("should return 200 and set cookies on valid credentials")
    void loginWithValidCredentials() throws Exception {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "user@example.com",
                    "password": "user"
                  }
                  """))
          .andExpect(status().isOk())
          .andExpect(cookie().exists("ACCESS_TOKEN"))
          .andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
          .andExpect(cookie().secure("ACCESS_TOKEN", true))
          .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("should reject invalid password with 401")
    void loginWithInvalidPassword() throws Exception {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "user@example.com",
                    "password": "wrongpassword"
                  }
                  """))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should reject non-existent user with 401")
    void loginWithNonExistentUser() throws Exception {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "nonexistent@example.com",
                    "password": "anypassword"
                  }
                  """))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should reject missing credentials with 400")
    void loginWithMissingCredentials() throws Exception {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /auth/logout")
  class LogoutTests {

    @Test
    @DisplayName("should return 200 and clear cookies when authenticated")
    void logoutWhenAuthenticated() throws Exception {
      MvcResult loginResult = mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "user@example.com",
                    "password": "user"
                  }
                  """))
          .andReturn();

      Cookie accessToken = loginResult.getResponse().getCookie("ACCESS_TOKEN");
      Cookie csrfToken = loginResult.getResponse().getCookie("XSRF-TOKEN");

      mockMvc.perform(post("/auth/logout")
              .cookie(accessToken, csrfToken)
              .header("X-XSRF-TOKEN", csrfToken.getValue()))
          .andExpect(status().isOk())
          .andExpect(cookie().maxAge("ACCESS_TOKEN", 0));
    }

    @Test
    @DisplayName("should return 403 without CSRF token")
    void logoutWithoutCsrfToken() throws Exception {
      MvcResult loginResult = mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "user@example.com",
                    "password": "user"
                  }
                  """))
          .andReturn();

      Cookie accessToken = loginResult.getResponse().getCookie("ACCESS_TOKEN");

      mockMvc.perform(post("/auth/logout")
              .cookie(accessToken))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /auth/me")
  class MeTests {

    @Test
    @DisplayName("should return user profile when authenticated")
    void getProfileWhenAuthenticated() throws Exception {
      MvcResult loginResult = mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "user@example.com",
                    "password": "user"
                  }
                  """))
          .andReturn();

      Cookie accessToken = loginResult.getResponse().getCookie("ACCESS_TOKEN");

      mockMvc.perform(get("/auth/me")
              .cookie(accessToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("user@example.com"))
          .andExpect(jsonPath("$.firstName").value("Demo"))
          .andExpect(jsonPath("$.lastName").value("User"))
          .andExpect(jsonPath("$.roles").isArray())
          .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("should reject unauthenticated request with 401")
    void getProfileWhenNotAuthenticated() throws Exception {
      mockMvc.perform(get("/auth/me"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should reject invalid token with 401")
    void getProfileWithInvalidToken() throws Exception {
      mockMvc.perform(get("/auth/me")
              .cookie(new Cookie("ACCESS_TOKEN", "invalid.jwt.token")))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return admin profile with admin roles")
    void getAdminProfile() throws Exception {
      MvcResult loginResult = mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "admin@example.com",
                    "password": "admin"
                  }
                  """))
          .andReturn();

      Cookie accessToken = loginResult.getResponse().getCookie("ACCESS_TOKEN");

      mockMvc.perform(get("/auth/me")
              .cookie(accessToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("admin@example.com"))
          .andExpect(jsonPath("$.roles").isArray())
          .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }
  }

  @Nested
  @DisplayName("CSRF Token Rotation")
  class CsrfTokenRotationTests {

    @Test
    @DisplayName("should return exactly one XSRF-TOKEN cookie on login")
    void loginReturnsSingleCsrfCookie() throws Exception {
      MvcResult result = mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "username": "user@example.com",
                    "password": "user"
                  }
                  """))
          .andExpect(status().isOk())
          .andReturn();

      long csrfCookieCount = Arrays.stream(result.getResponse().getCookies())
          .filter(c -> "XSRF-TOKEN".equals(c.getName()))
          .count();

      assertThat(csrfCookieCount).as("Expected exactly one XSRF-TOKEN cookie").isEqualTo(1);
    }

  }
}
