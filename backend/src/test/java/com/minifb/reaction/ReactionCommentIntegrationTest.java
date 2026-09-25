package com.minifb.reaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.minifb.support.Fixtures;
import com.minifb.support.Fixtures.TestUser;
import com.minifb.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class ReactionCommentIntegrationTest extends IntegrationTest {

  @Autowired Fixtures fixtures;

  // ---------- reaction ----------

  @Test
  void putReactionTwiceKeepsOneRowAndCountOne() throws Exception {
    TestUser author = fixtures.user();
    TestUser friend = fixtures.user();
    fixtures.befriend(author, friend);
    long postId = createPost(author, "bài");

    react(friend, postId, "LIKE")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reactionCount").value(1))
        .andExpect(jsonPath("$.myReaction").value("LIKE"));
    react(friend, postId, "LIKE").andExpect(jsonPath("$.reactionCount").value(1));

    assertThat(reactionRows(postId)).isOne();
    assertThat(reactionCount(postId)).isOne();
  }

  @Test
  void changingTypeDoesNotIncreaseCount() throws Exception {
    TestUser author = fixtures.user();
    long postId = createPost(author, "bài");

    react(author, postId, "LIKE");
    react(author, postId, "HAHA")
        .andExpect(jsonPath("$.reactionCount").value(1))
        .andExpect(jsonPath("$.myReaction").value("HAHA"));

    assertThat(reactionCount(postId)).isOne();
    assertThat(
            jdbc.queryForObject(
                "SELECT type FROM reactions WHERE post_id = ?", String.class, postId))
        .isEqualTo("HAHA");
    mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", author.bearer()))
        .andExpect(jsonPath("$.myReaction").value("HAHA"));
  }

  @Test
  void deleteReactionOnlyDecrementsWhenRemoved() throws Exception {
    TestUser author = fixtures.user();
    long postId = createPost(author, "bài");
    react(author, postId, "LOVE");

    unreact(author, postId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reactionCount").value(0))
        .andExpect(jsonPath("$.myReaction").doesNotExist());
    unreact(author, postId).andExpect(jsonPath("$.reactionCount").value(0));
    assertThat(reactionCount(postId)).isZero();
  }

  @Test
  void reactionRequiresViewPermissionAndValidType() throws Exception {
    TestUser author = fixtures.user();
    TestUser stranger = fixtures.user();
    long postId = createPost(author, "bài");

    react(stranger, postId, "LIKE").andExpect(status().isNotFound());
    unreact(stranger, postId).andExpect(status().isNotFound());
    react(author, postId, "DISLIKE").andExpect(status().isBadRequest());
    react(author, 999999, "LIKE").andExpect(status().isNotFound());
    mvc.perform(put("/api/v1/posts/" + postId + "/reaction")).andExpect(status().isUnauthorized());
    assertThat(reactionCount(postId)).isZero();
  }

  @Test
  void concurrentReactionsAndCommentsDoNotDeadlock() throws Exception {
    TestUser author = fixtures.user();
    long postId = createPost(author, "hot");
    int n = 8;
    List<TestUser> fans = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      TestUser fan = fixtures.user();
      fixtures.befriend(author, fan);
      fans.add(fan);
    }

    ExecutorService pool = Executors.newFixedThreadPool(n * 2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> results = new ArrayList<>();
    for (TestUser fan : fans) {
      Callable<Integer> reactCall =
          () -> {
            start.await();
            return react(fan, postId, "LOVE").andReturn().getResponse().getStatus();
          };
      Callable<Integer> commentCall =
          () -> {
            start.await();
            return comment(fan, postId, "wow").andReturn().getResponse().getStatus();
          };
      results.add(pool.submit(reactCall));
      results.add(pool.submit(commentCall));
    }
    start.countDown();
    for (Future<Integer> f : results) {
      assertThat(f.get()).isIn(200, 201);
    }
    pool.shutdown();

    assertThat(reactionCount(postId)).isEqualTo(n);
    assertThat(commentCount(postId)).isEqualTo(n);
  }

  // ---------- bình luận ----------

  @Test
  void commentCountStaysCorrectAfterAddAndDelete() throws Exception {
    TestUser author = fixtures.user();
    TestUser friend = fixtures.user();
    fixtures.befriend(author, friend);
    long postId = createPost(author, "bài");

    long c1 = commentId(comment(friend, postId, "  một  ").andExpect(status().isCreated()));
    comment(author, postId, "hai").andExpect(jsonPath("$.deletable").value(true));
    assertThat(commentCount(postId)).isEqualTo(2);

    deleteComment(friend, c1).andExpect(status().isNoContent());
    deleteComment(friend, c1).andExpect(status().isNotFound());
    assertThat(commentCount(postId)).isOne();
  }

  @Test
  void postAuthorCanDeleteOthersCommentButThirdPartyCannot() throws Exception {
    TestUser author = fixtures.user();
    TestUser friendA = fixtures.user();
    TestUser friendB = fixtures.user();
    TestUser stranger = fixtures.user();
    fixtures.befriend(author, friendA);
    fixtures.befriend(author, friendB);
    long postId = createPost(author, "bài");
    long commentByA =
        commentId(
            comment(friendA, postId, "của A").andExpect(jsonPath("$.content").value("của A")));

    deleteComment(friendB, commentByA).andExpect(status().isForbidden());
    deleteComment(stranger, commentByA).andExpect(status().isNotFound());
    deleteComment(author, commentByA).andExpect(status().isNoContent());
    assertThat(commentCount(postId)).isZero();
  }

  @Test
  void commentsAreListedOldestFirstWithCursor() throws Exception {
    TestUser author = fixtures.user();
    TestUser friend = fixtures.user();
    fixtures.befriend(author, friend);
    long postId = createPost(author, "bài");
    for (int i = 1; i <= 5; i++) {
      comment(i % 2 == 0 ? author : friend, postId, "c" + i);
    }

    MvcResult first =
        mvc.perform(
                get("/api/v1/posts/" + postId + "/comments")
                    .param("size", "3")
                    .header("Authorization", friend.bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andExpect(jsonPath("$.items[0].content").value("c1"))
            .andExpect(jsonPath("$.items[0].deletable").value(true))
            .andExpect(jsonPath("$.items[1].deletable").value(false))
            .andReturn();
    String cursor = JsonPath.read(body(first), "$.nextCursor");
    mvc.perform(
            get("/api/v1/posts/" + postId + "/comments")
                .param("size", "3")
                .param("cursor", cursor)
                .header("Authorization", friend.bearer()))
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].content").value("c4"))
        .andExpect(jsonPath("$.nextCursor").doesNotExist());
  }

  @Test
  void commentValidationAndVisibility() throws Exception {
    TestUser author = fixtures.user();
    TestUser stranger = fixtures.user();
    long postId = createPost(author, "bài");

    comment(author, postId, "   ").andExpect(status().isBadRequest());
    comment(author, postId, "x".repeat(1001)).andExpect(status().isBadRequest());
    comment(stranger, postId, "xin chào").andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/posts/" + postId + "/comments").header("Authorization", stranger.bearer()))
        .andExpect(status().isNotFound());
    assertThat(commentCount(postId)).isZero();
  }

  // ---------- helpers ----------

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

  private ResultActions react(TestUser user, long postId, String type) throws Exception {
    return mvc.perform(
        put("/api/v1/posts/" + postId + "/reaction")
            .header("Authorization", user.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"" + type + "\"}"));
  }

  private ResultActions unreact(TestUser user, long postId) throws Exception {
    return mvc.perform(
        delete("/api/v1/posts/" + postId + "/reaction").header("Authorization", user.bearer()));
  }

  private ResultActions comment(TestUser user, long postId, String content) throws Exception {
    return mvc.perform(
        post("/api/v1/posts/" + postId + "/comments")
            .header("Authorization", user.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"" + content + "\"}"));
  }

  private ResultActions deleteComment(TestUser user, long commentId) throws Exception {
    return mvc.perform(
        delete("/api/v1/comments/" + commentId).header("Authorization", user.bearer()));
  }

  private long commentId(ResultActions actions) throws Exception {
    return ((Number) JsonPath.read(body(actions.andReturn()), "$.id")).longValue();
  }

  private int reactionRows(long postId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM reactions WHERE post_id = ?", Integer.class, postId);
  }

  private int reactionCount(long postId) {
    return jdbc.queryForObject(
        "SELECT reaction_count FROM posts WHERE id = ?", Integer.class, postId);
  }

  private int commentCount(long postId) {
    return jdbc.queryForObject(
        "SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
  }

  private static String body(MvcResult result) throws Exception {
    return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }
}
