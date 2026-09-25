package com.minifb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minifb.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class SchemaIntegrationTest extends IntegrationTest {

  @Test
  void flywayCreatesAllTablesAndChecks() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
            String.class);
    assertThat(tables)
        .contains(
            "users",
            "refresh_tokens",
            "posts",
            "post_images",
            "comments",
            "reactions",
            "friendships");

    List<String> checks =
        jdbc.queryForList(
            "SELECT constraint_name FROM information_schema.check_constraints"
                + " WHERE constraint_schema = DATABASE()",
            String.class);
    assertThat(checks).contains("chk_friend_order", "chk_friend_requester");
  }

  @Test
  void friendshipCheckRejectsWrongOrder() {
    long a = insertUser("a@x.com");
    long b = insertUser("b@x.com");
    long low = Math.min(a, b);
    long high = Math.max(a, b);

    assertThatThrownBy(() -> insertFriendship(high, low, high))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("chk_friend_order");
  }

  @Test
  void friendshipCheckRejectsRequesterOutsidePair() {
    long a = insertUser("a@x.com");
    long b = insertUser("b@x.com");
    long c = insertUser("c@x.com");
    long low = Math.min(a, b);
    long high = Math.max(a, b);

    assertThatThrownBy(() -> insertFriendship(low, high, c))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("chk_friend_requester");
    insertFriendship(low, high, low);
  }

  private long insertUser(String email) {
    jdbc.update(
        "INSERT INTO users (email, password_hash, display_name, created_at, updated_at)"
            + " VALUES (?, 'x', 'User', NOW(6), NOW(6))",
        email);
    return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
  }

  private void insertFriendship(long low, long high, long requester) {
    jdbc.update(
        "INSERT INTO friendships (user_low, user_high, requester_id, status, created_at,"
            + " updated_at) VALUES (?, ?, ?, 'PENDING', NOW(6), NOW(6))",
        low,
        high,
        requester);
  }
}
