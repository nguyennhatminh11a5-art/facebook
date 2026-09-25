package com.minifb.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class UserIntegrationTest extends IntegrationTest {

  @Autowired Fixtures fixtures;
  @Autowired StorageService storage;

  @Test
  void updateProfileTrimsAndValidates() throws Exception {
    TestUser me = fixtures.user();
    patchMe(me, "{\"displayName\":\"  Tên mới \",\"bio\":\"  Xin chào  \"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Tên mới"))
        .andExpect(jsonPath("$.bio").value("Xin chào"));
    patchMe(me, "{\"displayName\":\"A\",\"bio\":\"   \"}").andExpect(jsonPath("$.bio").isEmpty());
    patchMe(me, "{\"displayName\":\"  \"}").andExpect(status().isBadRequest());
    patchMe(me, "{\"displayName\":\"" + "x".repeat(51) + "\"}").andExpect(status().isBadRequest());
    patchMe(me, "{\"displayName\":\"A\",\"bio\":\"" + "x".repeat(301) + "\"}")
        .andExpect(status().isBadRequest());
    mvc.perform(patch("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void profileShowsFriendStatusWithoutEmail() throws Exception {
    TestUser a = fixtures.user("An");
    TestUser b = fixtures.user("Bình");
    TestUser c = fixtures.user("Cường");
    TestUser d = fixtures.user("Dũng");
    fixtures.befriend(a, b);
    fixtures.request(a, c);
    fixtures.request(d, a);

    profile(a, b.id())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Bình"))
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.friendStatus").value("FRIENDS"));
    profile(a, c.id()).andExpect(jsonPath("$.friendStatus").value("OUTGOING"));
    profile(c, a.id()).andExpect(jsonPath("$.friendStatus").value("INCOMING"));
    profile(a, d.id()).andExpect(jsonPath("$.friendStatus").value("INCOMING"));
    profile(a, a.id()).andExpect(jsonPath("$.friendStatus").value("SELF"));
    profile(b, c.id()).andExpect(jsonPath("$.friendStatus").value("NONE"));
    profile(a, 999999).andExpect(status().isNotFound());
  }

  @Test
  void avatarUploadIsPublicAndOldFileIsDeleted() throws Exception {
    TestUser me = fixtures.user();
    String first = avatarUrl(uploadAvatar(me, TestImages.jpegWithExifGps(900, 600)));
    assertThat(first).startsWith("/api/v1/media/avatars/");

    byte[] served =
        mvc.perform(get(first))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(TestImages.hasApp1Segment(served)).isFalse();
    assertThat(TestImages.read(served).getHeight()).isEqualTo(640);

    String second = avatarUrl(uploadAvatar(me, TestImages.png(100, 100, true)));
    assertThat(second).endsWith(".png");
    String oldKey = first.substring(first.lastIndexOf('/') + 1);
    assertThat(storage.find(MediaBucket.AVATARS, oldKey)).isEmpty();
    mvc.perform(get(first)).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/users/me").header("Authorization", me.bearer()))
        .andExpect(jsonPath("$.avatarUrl").value(second));
  }

  @Test
  void avatarRejectsNonImage() throws Exception {
    TestUser me = fixtures.user();
    uploadAvatar(me, "hello".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest());
  }

  @Test
  void searchIsAccentInsensitiveAndEscapesWildcards() throws Exception {
    TestUser me = fixtures.user("Nguyễn Văn An");
    fixtures.user("Trần Thị Bình");
    fixtures.user("100% Real");
    fixtures.user("a_b");

    search(me, "nguyen")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].displayName").value("Nguyễn Văn An"));
    search(me, "BÌNH").andExpect(jsonPath("$.items", hasSize(1)));
    search(me, "%").andExpect(status().isBadRequest());
    search(me, "%%").andExpect(jsonPath("$.items", hasSize(0)));
    search(me, "0%").andExpect(jsonPath("$.items", hasSize(1)));
    search(me, "__").andExpect(jsonPath("$.items", hasSize(0)));
    search(me, "a_").andExpect(jsonPath("$.items", hasSize(1)));
    search(me, "\\\\").andExpect(jsonPath("$.items", hasSize(0)));
    search(me, "x".repeat(51)).andExpect(status().isBadRequest());
  }

  @Test
  void searchPaginatesById() throws Exception {
    TestUser me = fixtures.user("Tìm kiếm");
    for (int i = 0; i < 5; i++) {
      fixtures.user("Hoa " + i);
    }
    MvcResult first =
        mvc.perform(
                get("/api/v1/users/search")
                    .param("q", "hoa")
                    .param("size", "3")
                    .header("Authorization", me.bearer()))
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andReturn();
    String cursor = JsonPath.read(first.getResponse().getContentAsString(), "$.nextCursor");
    mvc.perform(
            get("/api/v1/users/search")
                .param("q", "hoa")
                .param("size", "3")
                .param("cursor", cursor)
                .header("Authorization", me.bearer()))
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].displayName").value("Hoa 3"));
  }

  @Test
  void escapeLikeHandlesSpecialCharacters() {
    assertThat(UserService.escapeLike("a%b_c\\d")).isEqualTo("a\\%b\\_c\\\\d");
    assertThat(List.of(UserService.escapeLike("plain"))).containsExactly("plain");
  }

  private ResultActions patchMe(TestUser user, String json) throws Exception {
    return mvc.perform(
        patch("/api/v1/users/me")
            .header("Authorization", user.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private ResultActions profile(TestUser viewer, long id) throws Exception {
    return mvc.perform(get("/api/v1/users/" + id).header("Authorization", viewer.bearer()));
  }

  private ResultActions uploadAvatar(TestUser user, byte[] data) throws Exception {
    return mvc.perform(
        multipart("/api/v1/users/me/avatar")
            .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", data))
            .header("Authorization", user.bearer()));
  }

  private ResultActions search(TestUser user, String q) throws Exception {
    return mvc.perform(
        get("/api/v1/users/search").param("q", q).header("Authorization", user.bearer()));
  }

  private static String avatarUrl(ResultActions actions) throws Exception {
    MvcResult result = actions.andExpect(status().isOk()).andReturn();
    return JsonPath.read(
        result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.avatarUrl");
  }
}
