CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name  VARCHAR(50)  NOT NULL,
  bio           VARCHAR(300),
  avatar_key    VARCHAR(255),
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL
);

CREATE TABLE refresh_tokens (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT      NOT NULL,
  family_id      CHAR(36)    NOT NULL,
  token_hash     CHAR(64)    NOT NULL UNIQUE,
  expires_at     DATETIME(6) NOT NULL,
  revoked_at     DATETIME(6),
  revoked_reason ENUM('ROTATED','LOGOUT','REUSE'),
  created_at     DATETIME(6) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_rt_family (family_id),
  INDEX idx_rt_expires (expires_at)
);

CREATE TABLE posts (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT NOT NULL,
  content        TEXT,
  reaction_count INT NOT NULL DEFAULT 0,
  comment_count  INT NOT NULL DEFAULT 0,
  created_at     DATETIME(6) NOT NULL,
  updated_at     DATETIME(6) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_posts_user_created (user_id, created_at DESC, id DESC)
);

CREATE TABLE post_images (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id       BIGINT            NOT NULL,
  storage_key   VARCHAR(255)      NOT NULL,
  thumb_key     VARCHAR(255)      NOT NULL,
  width         SMALLINT UNSIGNED NOT NULL,
  height        SMALLINT UNSIGNED NOT NULL,
  position      TINYINT           NOT NULL,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  UNIQUE KEY uq_post_image_pos (post_id, position)
);

CREATE TABLE comments (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  content    VARCHAR(1000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_comments_post_created (post_id, created_at, id)
);

CREATE TABLE reactions (
  post_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  type       ENUM('LIKE','LOVE','HAHA','WOW','SAD','ANGRY') NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (post_id, user_id),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Không CASCADE ở bảng này: v1 không có xoá tài khoản, và MySQL cấm CHECK trên cột có
-- ON DELETE/ON UPDATE. Đổi lại DB tự đảm bảo thứ tự cặp và người gửi hợp lệ.
CREATE TABLE friendships (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_low     BIGINT NOT NULL,
  user_high    BIGINT NOT NULL,
  requester_id BIGINT NOT NULL,
  status       ENUM('PENDING','ACCEPTED') NOT NULL,
  created_at   DATETIME(6) NOT NULL,
  updated_at   DATETIME(6) NOT NULL,
  FOREIGN KEY (user_low)     REFERENCES users(id),
  FOREIGN KEY (user_high)    REFERENCES users(id),
  FOREIGN KEY (requester_id) REFERENCES users(id),
  UNIQUE KEY uq_friend_pair (user_low, user_high),
  INDEX idx_friend_high (user_high, status),
  CONSTRAINT chk_friend_order     CHECK (user_low < user_high),
  CONSTRAINT chk_friend_requester CHECK (requester_id IN (user_low, user_high))
);
