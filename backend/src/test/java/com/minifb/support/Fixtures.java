package com.minifb.support;

import com.minifb.common.Times;
import com.minifb.security.AccessTokenService;
import com.minifb.user.User;
import com.minifb.user.UserRepository;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Tạo dữ liệu test nhanh, không đi qua endpoint có rate limit. */
@Component
public class Fixtures {

  public static final String PASSWORD = "password123";

  private static final AtomicInteger SEQ = new AtomicInteger();

  private final UserRepository users;
  private final AccessTokenService tokens;
  private final Clock clock;
  private final String passwordHash;

  public Fixtures(
      UserRepository users, AccessTokenService tokens, PasswordEncoder encoder, Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.clock = clock;
    this.passwordHash = encoder.encode(PASSWORD);
  }

  public record TestUser(long id, String email, String displayName, String token) {
    public String bearer() {
      return "Bearer " + token;
    }
  }

  public TestUser user(String displayName) {
    String email = "user" + SEQ.incrementAndGet() + "@test.com";
    User user = users.save(new User(email, passwordHash, displayName, Times.now(clock)));
    return new TestUser(user.getId(), email, displayName, tokens.issue(user.getId()));
  }

  public TestUser user() {
    return user("Người dùng " + (SEQ.get() + 1));
  }
}
