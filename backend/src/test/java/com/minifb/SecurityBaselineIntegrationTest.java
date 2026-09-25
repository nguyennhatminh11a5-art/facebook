package com.minifb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minifb.security.AccessTokenService;
import com.minifb.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecurityBaselineIntegrationTest extends IntegrationTest {

  @Autowired AccessTokenService tokens;

  @Test
  void validTokenPassesAuthentication() throws Exception {
    String token = tokens.issue(1);
    mvc.perform(get("/api/v1/does-not-exist").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void expiredTokenIsRejected() throws Exception {
    clock.advance(Duration.ofMinutes(-20));
    String token = tokens.issue(1);
    clock.reset();
    mvc.perform(get("/api/v1/does-not-exist").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void healthIsPublicAndHasSecurityHeaders() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "same-origin"))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    "default-src 'self'; img-src 'self' blob: data:; object-src 'none';"
                        + " frame-ancestors 'none'"));
  }

  @Test
  void apiRequiresAuthentication() throws Exception {
    mvc.perform(get("/api/v1/feed")).andExpect(status().isUnauthorized());
  }

  @Test
  void invalidBearerTokenIsRejected() throws Exception {
    mvc.perform(get("/api/v1/feed").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void swaggerIsDisabledOutsideDevProfile() throws Exception {
    mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
  }

  @Test
  void otherActuatorEndpointsAreNotExposed() throws Exception {
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }
}
