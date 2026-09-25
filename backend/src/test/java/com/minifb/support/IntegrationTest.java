package com.minifb.support;

import com.minifb.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Test tích hợp với MySQL thật (Testcontainers). Mọi test dùng chung một context/container. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

  @Autowired protected MockMvc mvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected MutableClock clock;
  @Autowired private RateLimiter rateLimiter;

  @BeforeEach
  void resetState() {
    for (String table :
        new String[] {
          "reactions", "comments", "post_images", "posts", "friendships", "refresh_tokens", "users"
        }) {
      jdbc.update("DELETE FROM " + table);
    }
    rateLimiter.reset();
    clock.reset();
  }
}
