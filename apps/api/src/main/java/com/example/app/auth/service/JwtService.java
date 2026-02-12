package com.example.app.auth.service;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final String PURPOSE_PASSWORD_RESET = "password-reset";

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final Clock clock;
  private final Duration accessTokenTtl;
  private final Duration passwordResetExchangeTokenTtl;
  private final String issuer;

  public JwtService(
      JwtEncoder encoder,
      JwtDecoder decoder,
      Clock clock,
      @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl,
      @Value("${security.jwt.password-reset-exchange-token-ttl:PT10M}") Duration passwordResetExchangeTokenTtl,
      @Value("${security.jwt.issuer}") String issuer
  ) {
    this.encoder = encoder;
    this.decoder = decoder;
    this.clock = clock;
    this.accessTokenTtl = accessTokenTtl;
    this.passwordResetExchangeTokenTtl = passwordResetExchangeTokenTtl;
    this.issuer = issuer;
  }

  public Duration getAccessTokenTtl() {
    return accessTokenTtl;
  }

  public String getIssuer() {
    return issuer;
  }

  public String createToken(String username, Collection<String> roles) {

    Instant now = clock.instant();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .id(UUID.randomUUID().toString())
        .issuedAt(now)
        .expiresAt(now.plus(accessTokenTtl))
        .subject(username)
        .claim("roles", roles)
        .build();

    return encoder
        .encode(JwtEncoderParameters.from(header, claims))
        .getTokenValue();
  }

  public String createPasswordResetToken(String email, String emailTokenHash) {
    Instant now = clock.instant();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .id(UUID.randomUUID().toString())
        .issuedAt(now)
        .expiresAt(now.plus(passwordResetExchangeTokenTtl))
        .subject(email)
        .claim("purpose", PURPOSE_PASSWORD_RESET)
        .claim("rst", emailTokenHash)
        .build();

    return encoder
        .encode(JwtEncoderParameters.from(header, claims))
        .getTokenValue();
  }

  public PasswordResetClaims parsePasswordResetToken(String token) {
    Jwt jwt;
    try {
      jwt = decoder.decode(token);
    } catch (JwtException e) {
      throw new IllegalArgumentException("Invalid or expired exchange token", e);
    }

    String purpose = jwt.getClaimAsString("purpose");
    if (!PURPOSE_PASSWORD_RESET.equals(purpose)) {
      throw new IllegalArgumentException("Invalid token purpose");
    }

    return new PasswordResetClaims(jwt.getSubject(), jwt.getClaimAsString("rst"));
  }

  public record PasswordResetClaims(String email, String emailTokenHash) {}
}
