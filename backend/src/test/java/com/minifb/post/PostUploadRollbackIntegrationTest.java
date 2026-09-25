package com.minifb.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minifb.media.MediaProperties;
import com.minifb.support.Fixtures;
import com.minifb.support.Fixtures.TestUser;
import com.minifb.support.IntegrationTest;
import com.minifb.support.TestImages;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Ghi DB thất bại sau khi đã lưu file → rollback → không còn file mồ côi. */
class PostUploadRollbackIntegrationTest extends IntegrationTest {

  @Autowired Fixtures fixtures;
  @Autowired MediaProperties mediaProperties;
  @MockitoSpyBean PostImageRepository postImages;

  @Test
  void rollbackDeletesStoredFiles() throws Exception {
    TestUser me = fixtures.user();
    long before = countFiles();
    doThrow(new IllegalStateException("DB down")).when(postImages).saveAll(anyIterable());

    mvc.perform(
            multipart("/api/v1/posts")
                .file(PostIntegrationTest.file("images", TestImages.jpeg(40, 40)))
                .file(PostIntegrationTest.file("images", TestImages.jpeg(40, 40)))
                .param("content", "lỗi")
                .header("Authorization", me.bearer()))
        .andExpect(status().isInternalServerError());

    assertThat(countFiles()).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM posts", Integer.class)).isZero();
  }

  private long countFiles() throws IOException {
    Path dir = Path.of(mediaProperties.root(), "posts");
    if (!Files.exists(dir)) {
      return 0;
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files.count();
    }
  }
}
