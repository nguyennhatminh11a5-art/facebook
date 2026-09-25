package com.minifb.friendship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.minifb.support.Fixtures;
import com.minifb.support.Fixtures.TestUser;
import com.minifb.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class FriendshipIntegrationTest extends IntegrationTest {

  @Autowired Fixtures fixtures;

  @Test
  void cannotBefriendSelfOrUnknownUser() throws Exception {
    TestUser a = fixtures.user();
    send(a, a.id()).andExpect(status().isBadRequest());
    send(a, 999999).andExpect(status().isNotFound());
    assertThat(rows()).isZero();
  }

  @Test
  void requestBackBecomesFriends() throws Exception {
    TestUser a = fixtures.user();
    TestUser b = fixtures.user();
    send(a, b.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.friendStatus").value("OUTGOING"));
    send(a, b.id()).andExpect(status().isConflict());
    send(b, a.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.friendStatus").value("FRIENDS"));
    send(a, b.id()).andExpect(status().isConflict());

    assertThat(rows()).isOne();
    assertThat(jdbc.queryForObject("SELECT status FROM friendships", String.class))
        .isEqualTo("ACCEPTED");
  }

  @Test
  void simultaneousRequestsBothWaysBecomeFriends() throws Exception {
    for (int round = 0; round < 3; round++) {
      TestUser a = fixtures.user();
      TestUser b = fixtures.user();
      ExecutorService pool = Executors.newFixedThreadPool(2);
      CountDownLatch start = new CountDownLatch(1);
      Callable<Integer> ab =
          () -> {
            start.await();
            return send(a, b.id()).andReturn().getResponse().getStatus();
          };
      Callable<Integer> ba =
          () -> {
            start.await();
            return send(b, a.id()).andReturn().getResponse().getStatus();
          };
      Future<Integer> f1 = pool.submit(ab);
      Future<Integer> f2 = pool.submit(ba);
      start.countDown();
      assertThat(List.of(f1.get(), f2.get())).containsOnly(200);
      pool.shutdown();

      String status =
          jdbc.queryForObject(
              "SELECT status FROM friendships WHERE user_low = ? AND user_high = ?",
              String.class,
              Math.min(a.id(), b.id()),
              Math.max(a.id(), b.id()));
      assertThat(status).isEqualTo("ACCEPTED");
    }
  }

  @Test
  void acceptOnlyIncomingRequest() throws Exception {
    TestUser a = fixtures.user();
    TestUser b = fixtures.user();
    send(a, b.id());

    accept(a, b.id()).andExpect(status().isNotFound());
    accept(b, a.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.friendStatus").value("FRIENDS"));
    accept(b, a.id()).andExpect(status().isNotFound());
  }

  @Test
  void declineAndCancelDeleteTheRow() throws Exception {
    TestUser a = fixtures.user();
    TestUser b = fixtures.user();
    send(a, b.id());
    mvc.perform(delete("/api/v1/friends/requests/" + a.id()).header("Authorization", b.bearer()))
        .andExpect(status().isNoContent());
    assertThat(rows()).isZero();

    send(a, b.id());
    mvc.perform(delete("/api/v1/friends/requests/" + b.id()).header("Authorization", a.bearer()))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/friends/requests/" + b.id()).header("Authorization", a.bearer()))
        .andExpect(status().isNotFound());
    assertThat(rows()).isZero();
  }

  @Test
  void unfriendMakesPostsNotFoundBothWays() throws Exception {
    TestUser a = fixtures.user();
    TestUser b = fixtures.user();
    fixtures.befriend(a, b);
    long postA = createPost(a, "của A");
    long postB = createPost(b, "của B");
    getPost(b, postA).andExpect(status().isOk());
    getPost(a, postB).andExpect(status().isOk());

    mvc.perform(delete("/api/v1/friends/" + b.id()).header("Authorization", a.bearer()))
        .andExpect(status().isNoContent());

    getPost(b, postA).andExpect(status().isNotFound());
    getPost(a, postB).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/feed").header("Authorization", a.bearer()))
        .andExpect(jsonPath("$.items", hasSize(1)));
    mvc.perform(delete("/api/v1/friends/" + b.id()).header("Authorization", a.bearer()))
        .andExpect(status().isNotFound());
  }

  @Test
  void listsFriendsAndRequests() throws Exception {
    TestUser me = fixtures.user();
    TestUser[] friends = {fixtures.user(), fixtures.user(), fixtures.user()};
    for (TestUser f : friends) {
      fixtures.befriend(me, f);
    }
    TestUser incoming = fixtures.user("Người gửi");
    TestUser outgoing = fixtures.user("Người nhận");
    fixtures.request(incoming, me);
    fixtures.request(me, outgoing);

    MvcResult page =
        mvc.perform(get("/api/v1/friends").param("size", "2").header("Authorization", me.bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.items[0].id").value(friends[0].id()))
            .andReturn();
    String cursor = JsonPath.read(body(page), "$.nextCursor");
    mvc.perform(
            get("/api/v1/friends")
                .param("size", "2")
                .param("cursor", cursor)
                .header("Authorization", me.bearer()))
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id").value(friends[2].id()));

    requests(me, "incoming")
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].user.displayName").value("Người gửi"));
    requests(me, "outgoing")
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].user.displayName").value("Người nhận"));
    requests(me, "sideways").andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/friends")).andExpect(status().isUnauthorized());
  }

  @Test
  void suggestionsAreFriendsOfFriendsOrderedByMutualCount() throws Exception {
    TestUser a = fixtures.user("A");
    TestUser b = fixtures.user("B");
    TestUser c = fixtures.user("C");
    TestUser d = fixtures.user("D");
    TestUser e = fixtures.user("E");
    TestUser pending = fixtures.user("F");
    fixtures.befriend(a, b);
    fixtures.befriend(a, e);
    fixtures.befriend(b, c);
    fixtures.befriend(e, c);
    fixtures.befriend(b, d);
    fixtures.befriend(b, pending);
    fixtures.request(a, pending);

    mvc.perform(get("/api/v1/friends/suggestions").header("Authorization", a.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].user.displayName").value("C"))
        .andExpect(jsonPath("$[0].mutualFriends").value(2))
        .andExpect(jsonPath("$[1].user.displayName").value("D"))
        .andExpect(jsonPath("$[1].mutualFriends").value(1));
  }

  // ---------- helpers ----------

  private ResultActions send(TestUser from, long toId) throws Exception {
    return mvc.perform(
        post("/api/v1/friends/requests")
            .header("Authorization", from.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":" + toId + "}"));
  }

  private ResultActions accept(TestUser me, long fromId) throws Exception {
    return mvc.perform(
        post("/api/v1/friends/requests/" + fromId + "/accept")
            .header("Authorization", me.bearer()));
  }

  private ResultActions requests(TestUser me, String direction) throws Exception {
    return mvc.perform(
        get("/api/v1/friends/requests")
            .param("direction", direction)
            .header("Authorization", me.bearer()));
  }

  private ResultActions getPost(TestUser viewer, long postId) throws Exception {
    return mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", viewer.bearer()));
  }

  private long createPost(TestUser user, String content) throws Exception {
    MvcResult result =
        mvc.perform(
                multipart("/api/v1/posts")
                    .param("content", content)
                    .header("Authorization", user.bearer()))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(body(result), "$.id")).longValue();
  }

  private int rows() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM friendships", Integer.class);
  }

  private static String body(MvcResult result) throws Exception {
    return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }
}
