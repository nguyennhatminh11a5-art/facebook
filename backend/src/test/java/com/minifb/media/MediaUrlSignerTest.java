package com.minifb.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MediaUrlSignerTest {

  private static final String SECRET =
      Base64.getEncoder()
          .encodeToString("test-only-media-secret-0000000000".getBytes(StandardCharsets.UTF_8));

  @Test
  void expiryIsNextHourPlusOne() {
    Instant now = Instant.parse("2026-01-01T10:59:59Z");
    assertThat(MediaUrlSigner.expiry(now))
        .isEqualTo(Instant.parse("2026-01-01T12:00:00Z").getEpochSecond());
    // Ngay đầu giờ vẫn sống đủ 2 giờ, và cùng giờ → cùng URL (cache được)
    assertThat(MediaUrlSigner.expiry(Instant.parse("2026-01-01T10:00:00Z")))
        .isEqualTo(MediaUrlSigner.expiry(now));
  }

  @Test
  void verifiesSignatureAndExpiry() {
    Instant now = Instant.parse("2026-01-01T10:30:00Z");
    MediaUrlSigner signer = signer(now);
    String url = signer.url("abc.jpg");
    long exp = Long.parseLong(url.replaceAll(".*exp=(\\d+).*", "$1"));
    String sig = url.replaceAll(".*sig=", "");

    assertThat(signer.verify("abc.jpg", exp, sig)).isTrue();
    assertThat(signer.verify("abd.jpg", exp, sig)).isFalse();
    assertThat(signer.verify("abc.jpg", exp + 3600, sig)).isFalse();
    assertThat(signer.verify("abc.jpg", exp, sig + "x")).isFalse();
    assertThat(signer.verify("abc.jpg", exp, null)).isFalse();

    MediaUrlSigner later = signer(Instant.ofEpochSecond(exp));
    assertThat(later.verify("abc.jpg", exp, sig)).isFalse();
  }

  private static MediaUrlSigner signer(Instant now) {
    return new MediaUrlSigner(
        new MediaProperties(SECRET, "uploads"), Clock.fixed(now, ZoneOffset.UTC));
  }
}
