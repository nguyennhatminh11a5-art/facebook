package com.minifb.friendship;

import com.minifb.common.PageResponse;
import com.minifb.friendship.dto.FriendRequestResponse;
import com.minifb.friendship.dto.FriendshipStatusResponse;
import com.minifb.friendship.dto.SendFriendRequest;
import com.minifb.friendship.dto.SuggestionResponse;
import com.minifb.security.CurrentUser;
import com.minifb.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
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

/** Mọi endpoint dùng userId của người kia làm định danh. */
@RestController
@RequestMapping("/api/v1/friends")
class FriendshipController {

  private final FriendshipService friendshipService;

  FriendshipController(FriendshipService friendshipService) {
    this.friendshipService = friendshipService;
  }

  @GetMapping
  PageResponse<UserSummary> friends(
      @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
    return friendshipService.friends(CurrentUser.id(), cursor, size);
  }

  @GetMapping("/requests")
  List<FriendRequestResponse> requests(@RequestParam(defaultValue = "incoming") String direction) {
    return friendshipService.requests(CurrentUser.id(), direction);
  }

  @PostMapping("/requests")
  FriendshipStatusResponse send(@Valid @RequestBody SendFriendRequest request) {
    return friendshipService.sendRequest(CurrentUser.id(), request.userId());
  }

  @PostMapping("/requests/{userId}/accept")
  FriendshipStatusResponse accept(@PathVariable long userId) {
    return friendshipService.accept(CurrentUser.id(), userId);
  }

  @DeleteMapping("/requests/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteRequest(@PathVariable long userId) {
    friendshipService.deleteRequest(CurrentUser.id(), userId);
  }

  @GetMapping("/suggestions")
  List<SuggestionResponse> suggestions() {
    return friendshipService.suggestions(CurrentUser.id());
  }

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void unfriend(@PathVariable long userId) {
    friendshipService.unfriend(CurrentUser.id(), userId);
  }
}
