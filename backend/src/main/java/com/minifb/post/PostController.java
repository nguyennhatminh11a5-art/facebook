package com.minifb.post;

import com.minifb.common.PageResponse;
import com.minifb.post.dto.PostResponse;
import com.minifb.post.dto.UpdatePostRequest;
import com.minifb.post.dto.UserPostsResponse;
import com.minifb.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
class PostController {

  private final PostService postService;

  PostController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  PostResponse create(
      @RequestParam(required = false) String content,
      @RequestParam(name = "images", required = false) List<MultipartFile> images) {
    return postService.create(CurrentUser.id(), content, images);
  }

  @GetMapping("/posts/{id}")
  PostResponse get(@PathVariable long id) {
    return postService.get(CurrentUser.id(), id);
  }

  @PatchMapping("/posts/{id}")
  PostResponse update(@PathVariable long id, @Valid @RequestBody UpdatePostRequest request) {
    return postService.updateContent(CurrentUser.id(), id, request.content());
  }

  @DeleteMapping("/posts/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable long id) {
    postService.delete(CurrentUser.id(), id);
  }

  @GetMapping("/feed")
  PageResponse<PostResponse> feed(
      @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
    return postService.feed(CurrentUser.id(), cursor, size);
  }

  @GetMapping("/users/{id}/posts")
  UserPostsResponse userPosts(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return postService.userPosts(CurrentUser.id(), id, cursor, size);
  }
}
