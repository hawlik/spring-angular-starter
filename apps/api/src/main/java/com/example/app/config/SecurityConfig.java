package com.example.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import com.example.app.auth.security.JwtCookieAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http,
      JwtCookieAuthenticationFilter jwtFilter
  ) throws Exception {

    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName(null);

    http
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .csrf(csrf -> csrf
            .spa()
            .csrfTokenRequestHandler(requestHandler)
            .sessionAuthenticationStrategy((authentication, request, response) -> {
              // No-op: prevent CsrfAuthenticationStrategy from rotating the token
              // on every authenticated request. The raw token in the cookie is stable
              // and doesn't need rotation for double-submit cookie security.
            })
            .ignoringRequestMatchers("/auth/login", "/auth/refresh", "/auth/forgot-password", "/auth/verify-reset-token")
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")
            )
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/login", "/auth/logout", "/auth/refresh",
                "/auth/forgot-password", "/auth/verify-reset-token", "/auth/reset-password")
            .permitAll()
            .requestMatchers("/admin/**")
            .hasRole("ADMIN")
            .anyRequest()
            .authenticated()
        )
        .addFilterBefore(
            jwtFilter,
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
  }
}
