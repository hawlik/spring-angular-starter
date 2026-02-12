package com.example.app.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

  @Bean
  SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String secret) {
    return new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSecretKey, @Value("${security.jwt.issuer}") String issuer) {
    return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
        .jwtProcessorCustomizer(processor -> {
          processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
              new JWTClaimsSet.Builder()
                  .issuer(issuer)
                  .build(),
              Set.of("sub", "iat", "exp")
          ));
        })
        .build();
  }
}
