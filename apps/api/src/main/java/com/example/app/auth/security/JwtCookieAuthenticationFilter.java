package com.example.app.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtCookieAuthenticationFilter.class);

  private final JwtDecoder jwtDecoder;

  public JwtCookieAuthenticationFilter(JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    if (request.getCookies() != null) {
      Optional<Cookie> tokenCookie = Arrays.stream(request.getCookies())
          .filter(c -> AuthCookieFactory.ACCESS_TOKEN_NAME.equals(c.getName()))
          .findFirst();

      tokenCookie.ifPresent(cookie -> {
        try {
          Jwt jwt = jwtDecoder.decode(cookie.getValue());

          List<SimpleGrantedAuthority> authorities = jwt.getClaimAsStringList("roles")
              .stream()
              .map(SimpleGrantedAuthority::new)
              .toList();

          var principal = User.withUsername(jwt.getSubject())
              .password("")
              .authorities(authorities)
              .build();

          UsernamePasswordAuthenticationToken auth =
              new UsernamePasswordAuthenticationToken(
                  principal, null, authorities);

          SecurityContextHolder.getContext()
              .setAuthentication(auth);

        } catch (JwtValidationException e) {
          log.debug("JWT validation failed: {}", e.getMessage());
          SecurityContextHolder.clearContext();
        } catch (BadJwtException e) {
          log.warn("Invalid JWT token received: {}", e.getMessage());
          SecurityContextHolder.clearContext();
        } catch (Exception e) {
          log.error("Unexpected error during JWT authentication", e);
          SecurityContextHolder.clearContext();
        }
      });
    }

    filterChain.doFilter(request, response);
  }
}
