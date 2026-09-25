package com.minifb.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minifb.support.Fixtures;
import com.minifb.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTest {

  @Autowired Fixtures fixtures;
  @Autowired AuthService authService;

  // ---------- đăng ký ----------

  @Test
  void registerCreatesUser() throws Exception {
    register("  Alice@Example.COM ", "password123", "Alice")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("alice@example.com"))
        .andExpect(jsonPath("$.displayName").value("Alice"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void registerDuplicateEmailIsConflict() throws Exception {
    register("bob@example.com", "password123", "Bob").andExpect(status().isCreated());
    register(" BOB@example.com", "password123", "Bob 2").andExpect(status().isConflict());
  }

  @Test
  void registerPasswordOver72BytesIsBadRequest() throws Exception {
    // 25 ký tự nhưng 75 byte UTF-8
    String password = "ệ".repeat(25);
    register("c@example.com", password, "C")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").exists());
  }

  @Test
  void registerValidatesFields() throws Exception {
    register("not-an-email", "short", " ").andExpect(status().isBadRequest());
    register("d@example.com", "password123", "x".repeat(51)).andExpect(status().isBadRequest());
  }

  @Test
  void registerIsRateLimitedPerIp() throws Exception {
    for (int i = 0; i < 5; i++) {
      register("r" + i + "@example.com", "password123", "R").andExpect(status().isCreated());
    }
    register("r5@example.com", "password123", "R")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  // ---------- đăng nhập ----------

  @Test
  void loginReturnsTokenAndSecureCookie() throws Exception {
    Fixtures.TestUser user = fixtures.user("Login");
    MvcResult result =
        login(user.email(), Fixtures.PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.user.id").value(user.id()))
            .andReturn();

    String setCookie = result.getResponse().getHeader("Set-Cookie");
    assertThat(setCookie)
        .contains("refresh_token=")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("SameSite=Strict")
        .contains("Path=/api/v1/auth");

    String token = com.jayway.jsonpath.JsonPath.read(body(result), "$.accessToken");
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(user.email()));
  }

  @Test
  void loginWrongPasswordOrUnknownEmailGivesSameMessage() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    login(user.email(), "wrong-password")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(AuthService.INVALID_CREDENTIALS));
    login("nobody@example.com", "whatever1")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(AuthService.INVALID_CREDENTIALS));
  }

  @Test
  void loginIsCaseInsensitiveOnEmail() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    login("  " + user.email().toUpperCase() + " ", Fixtures.PASSWORD).andExpect(status().isOk());
  }

  @Test
  void fiveFailedLoginsBlockEvenCorrectPassword() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    for (int i = 0; i < 5; i++) {
      login(user.email(), "wrong-password").andExpect(status().isUnauthorized());
    }
    login(user.email(), Fixtures.PASSWORD)
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void loginIsRateLimitedPerIp() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    for (int i = 0; i < 30; i++) {
      login(i % 2 == 0 ? user.email() : "x" + i + "@example.com", Fixtures.PASSWORD);
    }
    login(user.email(), Fixtures.PASSWORD).andExpect(status().isTooManyRequests());
  }

  // ---------- refresh ----------

  @Test
  void refreshRotatesToken() throws Exception {
    String first = loginCookie(fixtures.user());
    MockHttpServletResponse response =
        refresh(first).andExpect(status().isOk()).andReturn().getResponse();
    String second = cookieValue(response);
    assertThat(second).isNotBlank().isNotEqualTo(first);
    assertThat(reasonOf(first)).isEqualTo(RefreshToken.RevokedReason.ROTATED);

    refresh(second).andExpect(status().isOk());
  }

  @Test
  void refreshWithoutCookieIsUnauthorized() throws Exception {
    mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    refresh("garbage").andExpect(status().isUnauthorized());
  }

  @Test
  void reuseWithinGraceReturnsAccessTokenOnly() throws Exception {
    String first = loginCookie(fixtures.user());
    String second = cookieValue(refresh(first).andReturn().getResponse());

    clock.advance(Duration.ofSeconds(5));
    refresh(first)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(header().doesNotExist("Set-Cookie"));

    // không thu hồi gì: token mới vẫn dùng được
    refresh(second).andExpect(status().isOk());
  }

  @Test
  void concurrentRefreshWithSameTokenDoesNotLogOut() throws Exception {
    String token = loginCookie(fixtures.user());
    int threads = 2;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      Callable<Integer> call =
          () -> {
            start.await();
            return refresh(token).andReturn().getResponse().getStatus();
          };
      results.add(pool.submit(call));
    }
    start.countDown();
    for (Future<Integer> f : results) {
      assertThat(f.get()).isEqualTo(200);
    }
    pool.shutdown();
  }

  @Test
  void reuseAfterGraceRevokesWholeFamily() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    String first = loginCookie(user);
    String second = cookieValue(refresh(first).andReturn().getResponse());
    String otherDevice = loginCookie(user);

    clock.advance(Duration.ofSeconds(11));
    refresh(first)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

    assertThat(reasonOf(second)).isEqualTo(RefreshToken.RevokedReason.REUSE);
    refresh(second).andExpect(status().isUnauthorized());
    // phiên ở thiết bị khác không bị ảnh hưởng
    refresh(otherDevice).andExpect(status().isOk());
  }

  @Test
  void expiredRefreshTokenIsRejected() throws Exception {
    String token = loginCookie(fixtures.user());
    clock.advance(Duration.ofDays(8));
    refresh(token).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshIsRateLimitedPerIp() throws Exception {
    for (int i = 0; i < 30; i++) {
      refresh("garbage-" + i);
    }
    refresh("garbage").andExpect(status().isTooManyRequests());
  }

  // ---------- đăng xuất ----------

  @Test
  void logoutRevokesTokenAndClearsCookie() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    String token = loginCookie(user);
    mvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", user.bearer())
                .cookie(new Cookie(AuthController.REFRESH_COOKIE, token)))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

    assertThat(reasonOf(token)).isEqualTo(RefreshToken.RevokedReason.LOGOUT);
    refresh(token).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutCannotRevokeAnotherUsersToken() throws Exception {
    String victimToken = loginCookie(fixtures.user());
    Fixtures.TestUser attacker = fixtures.user();
    mvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", attacker.bearer())
                .cookie(new Cookie(AuthController.REFRESH_COOKIE, victimToken)))
        .andExpect(status().isNoContent());
    assertThat(reasonOf(victimToken)).isNull();
  }

  @Test
  void logoutRequiresAuthentication() throws Exception {
    mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
  }

  // ---------- dọn dẹp ----------

  @Test
  void cleanupDeletesTokensExpiredMoreThanOneDay() throws Exception {
    Fixtures.TestUser user = fixtures.user();
    String old = loginCookie(user);
    clock.advance(Duration.ofDays(9));
    String fresh = loginCookie(user);

    authService.deleteExpiredRefreshTokens();

    assertThat(countByToken(old)).isZero();
    assertThat(countByToken(fresh)).isOne();
  }

  @Test
  void meRequiresAuthenticationAndHidesPasswordHash() throws Exception {
    mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    Fixtures.TestUser user = fixtures.user();
    mvc.perform(get("/api/v1/users/me").header("Authorization", user.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(
            result -> assertThat(body(result)).doesNotContain("$2a$").doesNotContain("password"));
  }

  // ---------- helpers ----------

  private org.springframework.test.web.servlet.ResultActions register(
      String email, String password, String displayName) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":%s,"password":%s,"displayName":%s}
                """
                    .formatted(json(email), json(password), json(displayName))));
  }

  private org.springframework.test.web.servlet.ResultActions login(String email, String password)
      throws Exception {
    return mvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":%s,"password":%s}
                """
                    .formatted(json(email), json(password))));
  }

  private org.springframework.test.web.servlet.ResultActions refresh(String token)
      throws Exception {
    return mvc.perform(
        post("/api/v1/auth/refresh").cookie(new Cookie(AuthController.REFRESH_COOKIE, token)));
  }

  private String loginCookie(Fixtures.TestUser user) throws Exception {
    return cookieValue(
        login(user.email(), Fixtures.PASSWORD)
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie", not(containsString("Max-Age=0"))))
            .andReturn()
            .getResponse());
  }

  private static String cookieValue(MockHttpServletResponse response) {
    Cookie cookie = response.getCookie(AuthController.REFRESH_COOKIE);
    assertThat(cookie).isNotNull();
    return cookie.getValue();
  }

  private RefreshToken.RevokedReason reasonOf(String raw) {
    String reason =
        jdbc.queryForObject(
            "SELECT revoked_reason FROM refresh_tokens WHERE token_hash = ?",
            String.class,
            AuthService.hash(raw));
    return reason == null ? null : RefreshToken.RevokedReason.valueOf(reason);
  }

  private int countByToken(String raw) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?",
        Integer.class,
        AuthService.hash(raw));
  }

  private static String body(MvcResult result) throws Exception {
    return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String json(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
