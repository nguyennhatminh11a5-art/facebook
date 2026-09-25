package com.minifb.reaction;

import com.minifb.reaction.dto.ReactRequest;
import com.minifb.reaction.dto.ReactionSummary;
import com.minifb.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts/{postId}/reaction")
class ReactionController {

  private final ReactionService reactionService;

  ReactionController(ReactionService reactionService) {
    this.reactionService = reactionService;
  }

  @PutMapping
  ReactionSummary react(@PathVariable long postId, @Valid @RequestBody ReactRequest request) {
    return reactionService.react(CurrentUser.id(), postId, request.type());
  }

  @DeleteMapping
  ReactionSummary unreact(@PathVariable long postId) {
    return reactionService.unreact(CurrentUser.id(), postId);
  }
}
