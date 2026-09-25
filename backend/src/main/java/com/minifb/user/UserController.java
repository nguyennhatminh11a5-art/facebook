package com.minifb.user;

import com.minifb.security.CurrentUser;
import com.minifb.user.dto.MeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
