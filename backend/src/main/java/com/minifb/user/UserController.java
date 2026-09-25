package com.minifb.user;

import com.minifb.common.PageResponse;
import com.minifb.security.CurrentUser;
import com.minifb.user.dto.MeResponse;
import com.minifb.user.dto.ProfileResponse;
import com.minifb.user.dto.UpdateProfileRequest;
import com.minifb.user.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  MeResponse me() {
    return userService.me(CurrentUser.id());
  }

  @PatchMapping("/me")
  MeResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateMe(CurrentUser.id(), request);
  }

  @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  MeResponse changeAvatar(@RequestPart("file") MultipartFile file) {
    return userService.changeAvatar(CurrentUser.id(), file);
  }

  @GetMapping("/search")
  PageResponse<UserSummary> search(
      @RequestParam String q,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return userService.search(q, cursor, size);
  }

  @GetMapping("/{id}")
  ProfileResponse profile(@PathVariable long id) {
    return userService.profile(CurrentUser.id(), id);
  }
}
