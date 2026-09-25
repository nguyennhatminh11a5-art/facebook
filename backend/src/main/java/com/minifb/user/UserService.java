package com.minifb.user;

import com.minifb.common.NotFoundException;
import com.minifb.user.dto.MeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository users;

  public UserService(UserRepository users) {
    this.users = users;
  }

  @Transactional(readOnly = true)
  public MeResponse me(long currentUserId) {
    return MeResponse.of(require(currentUserId));
  }

  User require(long id) {
    return users.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
  }
}
