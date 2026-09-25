package com.minifb.comment;

import com.minifb.comment.dto.CommentRequest;
import com.minifb.comment.dto.CommentResponse;
import com.minifb.common.PageResponse;
import com.minifb.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class CommentController {

  private final CommentService commentService;

  CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/posts/{postId}/comments")
  PageResponse<CommentResponse> list(
      @PathVariable long postId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return commentService.list(CurrentUser.id(), postId, cursor, size);
  }

  @PostMapping("/posts/{postId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  CommentResponse add(@PathVariable long postId, @Valid @RequestBody CommentRequest request) {
    return commentService.add(CurrentUser.id(), postId, request.content());
  }

  @DeleteMapping("/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable long id) {
    commentService.delete(CurrentUser.id(), id);
  }
}
