package com.minifb.media;

import com.minifb.security.Secrets;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * URL ký HMAC-SHA256 cho ảnh bài viết (mục 5.4). {@code exp} = giờ tròn kế tiếp + 1 giờ: URL giống
 * nhau trong cùng một giờ (trình duyệt cache được), sống 1–2 giờ, không hết hạn ngay khi vừa tạo.
 */
@Component
public class MediaUrlSigner {

  public static final String PREFIX = "/api/v1/media/";

  private final SecretKeySpec key;
  private final Clock clock;

  public MediaUrlSigner(MediaProperties props, Clock clock) {
    this.key =
        new SecretKeySpec(
            Secrets.decode("MEDIA_SIGNING_SECRET", props.signingSecret()), "HmacSHA256");
    this.clock = clock;
  }

  public String url(String storageKey) {
    long exp = expiry(Instant.now(clock));
    return PREFIX + storageKey + "?exp=" + exp + "&sig=" + sign(storageKey, exp);
  }

  public boolean verify(String storageKey, long exp, String sig) {
    if (sig == null || exp <= Instant.now(clock).getEpochSecond()) {
      return false;
    }
    byte[] expected = sign(storageKey, exp).getBytes(StandardCharsets.US_ASCII);
    // So sánh thời gian hằng
    return MessageDigest.isEqual(expected, sig.getBytes(StandardCharsets.US_ASCII));
  }

  static long expiry(Instant now) {
    return now.truncatedTo(ChronoUnit.HOURS).plus(Duration.ofHours(2)).getEpochSecond();
  }

  private String sign(String storageKey, long exp) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      byte[] digest = mac.doFinal((storageKey + "|" + exp).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
