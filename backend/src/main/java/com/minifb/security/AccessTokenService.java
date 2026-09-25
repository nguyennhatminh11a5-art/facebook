package com.minifb.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Cấp access token 15 phút, {@code sub} = id người dùng. */
@Component
public class AccessTokenService {

  private final JwtEncoder encoder;
  private final JwtProperties props;
  private final Clock clock;

  public AccessTokenService(JwtEncoder encoder, JwtProperties props, Clock clock) {
    this.encoder = encoder;
    this.props = props;
    this.clock = clock;
  }

  public String issue(long userId) {
    Instant now = Instant.now(clock);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(Long.toString(userId))
            .issuedAt(now)
            .expiresAt(now.plus(props.accessTtl()))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
