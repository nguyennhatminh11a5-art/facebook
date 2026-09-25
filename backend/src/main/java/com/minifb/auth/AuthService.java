package com.minifb.auth;

import com.minifb.auth.RefreshToken.RevokedReason;
import com.minifb.auth.dto.LoginRequest;
import com.minifb.auth.dto.RegisterRequest;
import com.minifb.common.ConflictException;
import com.minifb.common.Times;
import com.minifb.common.UnauthorizedException;
import com.minifb.security.AccessTokenService;
import com.minifb.security.RateLimiter;
import com.minifb.security.RateLimiter.Limit;
import com.minifb.user.User;
import com.minifb.user.UserRepository;
import com.minifb.user.dto.MeResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đăng ký, đăng nhập, refresh (rotation theo family, mục 6.1), đăng xuất. */
@Service
public class AuthService {

  static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng";
  static final String INVALID_SESSION = "Phiên đăng nhập không hợp lệ";
  private static final int BCRYPT_MAX_BYTES = 72;

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenService accessTokens;
  private final RateLimiter rateLimiter;
  private final AuthProperties props;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  /** Dùng khi email không tồn tại để thời gian phản hồi giống khi sai mật khẩu. */
  private final String dummyHash;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      AccessTokenService accessTokens,
      RateLimiter rateLimiter,
      AuthProperties props,
      Clock clock) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.accessTokens = accessTokens;
    this.rateLimiter = rateLimiter;
    this.props = props;
    this.clock = clock;
    this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
  }

  public static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  @Transactional
  public MeResponse register(RegisterRequest request, String ip) {
    rateLimiter.consume(Limit.REGISTER_IP, ip);
    String email = normalizeEmail(request.email());
    if (users.existsByEmail(email)) {
      throw new ConflictException("Email đã được sử dụng");
    }
    User user =
        new User(
            email,
            passwordEncoder.encode(request.password()),
            request.displayName().trim(),
            Times.now(clock));
    try {
      users.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      // Hai request đăng ký cùng email đồng thời
      throw new ConflictException("Email đã được sử dụng");
    }
    return MeResponse.of(user);
  }

  @Transactional
  public AuthResult login(LoginRequest request, String ip) {
    rateLimiter.consume(Limit.LOGIN_IP, ip);
    String email = normalizeEmail(request.email());
    String failureKey = email + "|" + ip;
    rateLimiter.ensureAvailable(Limit.LOGIN_FAILURE_EMAIL_IP, failureKey);

    Optional<User> user = users.findByEmail(email);
    boolean passwordOk = checkPassword(request.password(), user.map(User::getPasswordHash));
    if (user.isEmpty() || !passwordOk) {
      rateLimiter.consumeQuietly(Limit.LOGIN_FAILURE_EMAIL_IP, failureKey);
      throw new UnauthorizedException(INVALID_CREDENTIALS);
    }

    User u = user.get();
    String raw = issueRefreshToken(u.getId(), UUID.randomUUID().toString(), Times.now(clock));
    return new AuthResult(accessTokens.issue(u.getId()), MeResponse.of(u), raw);
  }

  /**
   * Rotation theo family. Không rollback khi trả 401 vì việc thu hồi cả family phải được lưu lại.
   */
  @Transactional(noRollbackFor = UnauthorizedException.class)
  public AuthResult refresh(String rawToken, String ip) {
    rateLimiter.consume(Limit.REFRESH_IP, ip);
    if (rawToken == null || rawToken.isBlank()) {
      throw new UnauthorizedException(INVALID_SESSION);
    }
    RefreshToken token =
        refreshTokens
            .findByHashForUpdate(hash(rawToken))
            .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
    Instant now = Times.now(clock);

    if (!token.getExpiresAt().isAfter(now)) {
      throw new UnauthorizedException(INVALID_SESSION);
    }

    if (token.getRevokedAt() == null) {
      token.revoke(RevokedReason.ROTATED, now);
      String raw = issueRefreshToken(token.getUserId(), token.getFamilyId(), now);
      return authResult(token.getUserId(), raw);
    }

    boolean withinGrace =
        token.getRevokedReason() == RevokedReason.ROTATED
            && !now.isAfter(token.getRevokedAt().plus(props.reuseGrace()));
    if (withinGrace && refreshTokens.familyHasActiveToken(token.getFamilyId(), now)) {
      // 2 tab refresh cùng lúc: chỉ cấp access token, không đổi cookie, không thu hồi gì
      return authResult(token.getUserId(), null);
    }

    int revoked = refreshTokens.revokeFamily(token.getFamilyId(), now);
    log.warn(
        "Refresh token reuse detected: userId={}, revokedInFamily={}", token.getUserId(), revoked);
    throw new UnauthorizedException(INVALID_SESSION);
  }

  /** Thu hồi refresh token của phiên hiện tại (nếu cookie thuộc về chính người dùng). */
  @Transactional
  public void logout(String rawToken, long currentUserId) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByHashForUpdate(hash(rawToken))
        .filter(t -> t.getUserId() == currentUserId && t.getRevokedAt() == null)
        .ifPresent(t -> t.revoke(RevokedReason.LOGOUT, Times.now(clock)));
  }

  /** Hằng ngày xoá refresh token đã hết hạn quá 1 ngày. */
  @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
  @Transactional
  public void deleteExpiredRefreshTokens() {
    int deleted = refreshTokens.deleteExpiredBefore(Times.now(clock).minus(Duration.ofDays(1)));
    log.info("Deleted {} expired refresh tokens", deleted);
  }

  private AuthResult authResult(long userId, String newRawToken) {
    User user =
        users.findById(userId).orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
    return new AuthResult(accessTokens.issue(userId), MeResponse.of(user), newRawToken);
  }

  private boolean checkPassword(String password, Optional<String> hash) {
    if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
      passwordEncoder.matches("x", dummyHash);
      return false;
    }
    return passwordEncoder.matches(password, hash.orElse(dummyHash)) && hash.isPresent();
  }

  private String issueRefreshToken(long userId, String familyId, Instant now) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    refreshTokens.save(
        new RefreshToken(userId, familyId, hash(raw), now.plus(props.refreshTtl()), now));
    return raw;
  }

  static String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
