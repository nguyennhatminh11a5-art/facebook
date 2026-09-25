package com.minifb.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.minifb.media.MediaBucket;
import com.minifb.media.StorageService;
import com.minifb.support.Fixtures;
import com.minifb.support.Fixtures.TestUser;
import com.minifb.support.IntegrationTest;
import com.minifb.support.TestImages;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

class PostIntegrationTest extends IntegrationTest {

  @Autowired Fixtures fixtures;
  @Autowired StorageService storage;

  // ---------- tạo bài ----------

  @Test
  void createTextPost() throws Exception {
    TestUser me = fixtures.user("Tác giả");
    createPost(me, "  Xin chào  ")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("Xin chào"))
        .andExpect(jsonPath("$.author.id").value(me.id()))
        .andExpect(jsonPath("$.author.displayName").value("Tác giả"))
        .andExpect(jsonPath("$.images", hasSize(0)))
        .andExpect(jsonPath("$.reactionCount").value(0))
        .andExpect(jsonPath("$.myReaction").doesNotExist())
        .andExpect(jsonPath("$.editable").value(true))
        .andExpect(jsonPath("$.createdAt", startsWith("20")));
  }

  @Test
  void createPostWithImageStripsGpsAndServesSignedUrl() throws Exception {
    TestUser me = fixtures.user();
    MvcResult result =
        createPost(me, null, file("images", TestImages.jpegWithExifGps(200, 100)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.images", hasSize(1)))
            .andExpect(jsonPath("$.images[0].width").value(100))
            .andExpect(jsonPath("$.images[0].height").value(200))
            .andReturn();
    String url = JsonPath.read(body(result), "$.images[0].url");
    String thumbUrl = JsonPath.read(body(result), "$.images[0].thumbUrl");
    assertThat(url).startsWith("/api/v1/media/").contains("exp=").contains("sig=");

    byte[] served =
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Cache-Control", "max-age=3600, private"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(TestImages.hasApp1Segment(served)).isFalse();
    mvc.perform(get(thumbUrl)).andExpect(status().isOk());
  }

  @Test
  void fakeImageIsBadRequest() throws Exception {
    TestUser me = fixtures.user();
    byte[] fake = "not an image".getBytes(StandardCharsets.UTF_8);
    createPost(me, "hi", new MockMultipartFile("images", "a.jpg", "image/jpeg", fake))
        .andExpect(status().isBadRequest());
  }

  @Test
  void over40MegapixelIsBadRequest() throws Exception {
    TestUser me = fixtures.user();
    createPost(me, "hi", file("images", TestImages.pngHeaderOnly(8000, 6000)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validationRules() throws Exception {
    TestUser me = fixtures.user();
    createPost(me, "   ").andExpect(status().isBadRequest());
    createPost(me, "x".repeat(5001)).andExpect(status().isBadRequest());
    byte[] img = TestImages.jpeg(10, 10);
    createPost(
            me,
            "5 ảnh",
            file("images", img),
            file("images", img),
            file("images", img),
            file("images", img),
            file("images", img))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createRequiresAuthentication() throws Exception {
    mvc.perform(multipart("/api/v1/posts").param("content", "x"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createPostIsRateLimited() throws Exception {
    TestUser me = fixtures.user();
    for (int i = 0; i < 30; i++) {
      createPost(me, "bài " + i).andExpect(status().isCreated());
    }
    createPost(me, "bài 31").andExpect(status().isTooManyRequests());
  }

  // ---------- quyền xem/sửa/xoá ----------

  @Test
  void friendCanViewButNotEditOrDelete() throws Exception {
    TestUser author = fixtures.user();
    TestUser friend = fixtures.user();
    fixtures.befriend(author, friend);
    long postId = postId(createPost(author, "bài của tôi"));

    mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", friend.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editable").value(false));
    patchPost(friend, postId, "sửa trộm").andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/posts/" + postId).header("Authorization", friend.bearer()))
        .andExpect(status().isForbidden());
  }

  @Test
  void strangerGetsNotFound() throws Exception {
    TestUser author = fixtures.user();
    TestUser stranger = fixtures.user();
    TestUser pending = fixtures.user();
    fixtures.request(pending, author);
    long postId = postId(createPost(author, "riêng tư"));

    for (TestUser viewer : List.of(stranger, pending)) {
      mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", viewer.bearer()))
          .andExpect(status().isNotFound());
      patchPost(viewer, postId, "x").andExpect(status().isNotFound());
      mvc.perform(delete("/api/v1/posts/" + postId).header("Authorization", viewer.bearer()))
          .andExpect(status().isNotFound());
    }
    mvc.perform(get("/api/v1/posts/999999").header("Authorization", stranger.bearer()))
        .andExpect(status().isNotFound());
  }

  @Test
  void authorEditsContent() throws Exception {
    TestUser me = fixtures.user();
    long textPost = postId(createPost(me, "cũ"));
    patchPost(me, textPost, "  mới  ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("mới"));
    patchPost(me, textPost, "   ").andExpect(status().isBadRequest());

    long imagePost = postId(createPost(me, "có ảnh", file("images", TestImages.jpeg(20, 20))));
    patchPost(me, imagePost, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").doesNotExist());
  }

  @Test
  void deleteRemovesPostAndFilesAfterCommit() throws Exception {
    TestUser me = fixtures.user();
    MvcResult created =
        createPost(me, "xoá", file("images", TestImages.jpeg(30, 30)))
            .andExpect(status().isCreated())
            .andReturn();
    long postId = ((Number) JsonPath.read(body(created), "$.id")).longValue();
    List<String> keys = imageKeys(postId);
    assertThat(keys).hasSize(2);
    keys.forEach(k -> assertThat(storage.find(MediaBucket.POSTS, k)).isPresent());

    mvc.perform(delete("/api/v1/posts/" + postId).header("Authorization", me.bearer()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", me.bearer()))
        .andExpect(status().isNotFound());
    keys.forEach(k -> assertThat(storage.find(MediaBucket.POSTS, k)).isEmpty());
  }

  // ---------- URL ký ----------

  @Test
  void signedUrlTamperedOrExpiredIsNotFound() throws Exception {
    TestUser me = fixtures.user();
    MvcResult created = createPost(me, null, file("images", TestImages.jpeg(30, 30))).andReturn();
    String url = JsonPath.read(body(created), "$.images[0].url");
    String key = url.substring("/api/v1/media/".length(), url.indexOf('?'));

    mvc.perform(get(url.replaceAll("sig=.", "sig=A"))).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/media/" + key)).andExpect(status().isNotFound());
    // Ảnh bài viết không đọc được qua endpoint avatar công khai
    mvc.perform(get("/api/v1/media/avatars/" + key)).andExpect(status().isNotFound());

    clock.advance(Duration.ofHours(3));
    mvc.perform(get(url)).andExpect(status().isNotFound());
  }

  // ---------- bảng tin ----------

  @Test
  void feedShowsOnlyMineAndFriendsPosts() throws Exception {
    TestUser me = fixtures.user();
    TestUser friend = fixtures.user();
    TestUser stranger = fixtures.user();
    TestUser pending = fixtures.user();
    fixtures.befriend(me, friend);
    fixtures.request(me, pending);
    createPost(me, "của tôi");
    createPost(friend, "của bạn");
    createPost(stranger, "người lạ");
    createPost(pending, "chưa là bạn");

    mvc.perform(get("/api/v1/feed").header("Authorization", me.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].content").value("của bạn"))
        .andExpect(jsonPath("$.items[1].content").value("của tôi"))
        .andExpect(jsonPath("$.nextCursor").doesNotExist());
  }

  @Test
  void feedPaginationHasNoDuplicatesOrGapsWhenNewPostsArrive() throws Exception {
    TestUser me = fixtures.user();
    TestUser friend = fixtures.user();
    fixtures.befriend(me, friend);
    List<Long> created = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      created.add(postId(createPost(i % 2 == 0 ? me : friend, "bài " + i)));
    }
    // Cùng created_at → thứ tự theo id
    jdbc.update(
        "UPDATE posts SET created_at = (SELECT t.c FROM (SELECT MIN(created_at) c FROM posts) t)");

    Set<Long> seen = new HashSet<>();
    String cursor = null;
    int pages = 0;
    do {
      var request = get("/api/v1/feed").param("size", "3").header("Authorization", me.bearer());
      if (cursor != null) {
        request.param("cursor", cursor);
      }
      MvcResult page = mvc.perform(request).andExpect(status().isOk()).andReturn();
      List<Integer> ids = JsonPath.read(body(page), "$.items[*].id");
      for (Integer id : ids) {
        assertThat(seen.add(id.longValue())).as("trùng bài %s", id).isTrue();
      }
      cursor = JsonPath.read(body(page), "$.nextCursor");
      if (pages == 0) {
        // Bài mới chen vào sau khi tải trang đầu
        createPost(friend, "bài mới");
      }
      pages++;
    } while (cursor != null);

    assertThat(seen).containsExactlyInAnyOrderElementsOf(created);
  }

  @Test
  void feedRejectsBadCursorAndSize() throws Exception {
    TestUser me = fixtures.user();
    mvc.perform(get("/api/v1/feed").param("cursor", "!!!").header("Authorization", me.bearer()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/feed").param("size", "51").header("Authorization", me.bearer()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/feed")).andExpect(status().isUnauthorized());
  }

  // ---------- bài của một người ----------

  @Test
  void userPostsRespectVisibility() throws Exception {
    TestUser author = fixtures.user();
    TestUser friend = fixtures.user();
    TestUser stranger = fixtures.user();
    fixtures.befriend(author, friend);
    createPost(author, "một");
    createPost(author, "hai");

    mvc.perform(
            get("/api/v1/users/" + author.id() + "/posts").header("Authorization", friend.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.friendStatus").value("FRIENDS"));
    mvc.perform(
            get("/api/v1/users/" + author.id() + "/posts")
                .header("Authorization", stranger.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)))
        .andExpect(jsonPath("$.friendStatus").value("NONE"));
    mvc.perform(
            get("/api/v1/users/" + author.id() + "/posts").header("Authorization", author.bearer()))
        .andExpect(jsonPath("$.friendStatus").value("SELF"));
    mvc.perform(get("/api/v1/users/999999/posts").header("Authorization", author.bearer()))
        .andExpect(status().isNotFound());
  }

  // ---------- helpers ----------

  ResultActions createPost(TestUser user, String content, MockMultipartFile... files)
      throws Exception {
    MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/posts");
    for (MockMultipartFile f : files) {
      request.file(f);
    }
    if (content != null) {
      request.param("content", content);
    }
    return mvc.perform(request.header("Authorization", user.bearer()));
  }

  private ResultActions patchPost(TestUser user, long postId, String content) throws Exception {
    return mvc.perform(
        patch("/api/v1/posts/" + postId)
            .header("Authorization", user.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"" + content + "\"}"));
  }

  static MockMultipartFile file(String name, byte[] data) {
    return new MockMultipartFile(name, "photo.jpg", "image/jpeg", data);
  }

  private long postId(ResultActions actions) throws Exception {
    return ((Number) JsonPath.read(body(actions.andReturn()), "$.id")).longValue();
  }

  private List<String> imageKeys(long postId) {
    List<String> keys = new ArrayList<>();
    jdbc.query(
        "SELECT storage_key, thumb_key FROM post_images WHERE post_id = ?",
        rs -> {
          keys.add(rs.getString(1));
          keys.add(rs.getString(2));
        },
        postId);
    return keys;
  }

  static String body(MvcResult result) throws Exception {
    return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }
}
