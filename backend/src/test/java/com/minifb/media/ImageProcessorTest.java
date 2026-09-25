package com.minifb.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minifb.common.BadRequestException;
import com.minifb.support.TestImages;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ImageProcessorTest {

  private final ImageProcessor processor = new ImageProcessor();

  @Test
  void jpegIsReencodedWithThumbnail() {
    ProcessedImage out = processor.process(TestImages.jpeg(300, 200));
    assertThat(out.extension()).isEqualTo("jpg");
    assertThat(out.width()).isEqualTo(300);
    assertThat(out.height()).isEqualTo(200);
    assertThat(TestImages.read(out.thumbnail()).getWidth()).isEqualTo(300);
  }

  @Test
  void largeImageIsResizedTo2048AndThumbTo640() {
    ProcessedImage out = processor.process(TestImages.jpeg(4000, 1000));
    assertThat(out.width()).isEqualTo(2048);
    assertThat(out.height()).isEqualTo(512);
    BufferedImage thumb = TestImages.read(out.thumbnail());
    assertThat(thumb.getWidth()).isEqualTo(640);
    assertThat(thumb.getHeight()).isEqualTo(160);
  }

  @Test
  void exifGpsIsStrippedAndOrientationApplied() {
    byte[] input = TestImages.jpegWithExifGps(200, 100);
    assertThat(TestImages.hasApp1Segment(input)).isTrue();

    ProcessedImage out = processor.process(input);

    assertThat(TestImages.hasApp1Segment(out.main())).isFalse();
    assertThat(TestImages.hasApp1Segment(out.thumbnail())).isFalse();
    assertThat(new String(out.main(), StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    // Orientation = 6 → xoay 90°: 200x100 thành 100x200
    assertThat(out.width()).isEqualTo(100);
    assertThat(out.height()).isEqualTo(200);
  }

  @Test
  void pngWithAlphaStaysPng() {
    ProcessedImage out = processor.process(TestImages.png(50, 40, true));
    assertThat(out.extension()).isEqualTo("png");
  }

  @Test
  void opaquePngBecomesJpeg() {
    assertThat(processor.process(TestImages.png(50, 40, false)).extension()).isEqualTo("jpg");
  }

  @Test
  void fakeImageIsRejected() {
    byte[] text = "<?php echo 'hi'; ?>".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> processor.process(text)).isInstanceOf(BadRequestException.class);
  }

  @Test
  void truncatedJpegIsRejected() {
    byte[] jpeg = TestImages.jpeg(100, 100);
    byte[] truncated = java.util.Arrays.copyOf(jpeg, 20);
    assertThatThrownBy(() -> processor.process(truncated)).isInstanceOf(BadRequestException.class);
  }

  @Test
  void over40MegapixelIsRejectedBeforeDecoding() {
    byte[] bomb = TestImages.pngHeaderOnly(10_000, 5_000);
    assertThatThrownBy(() -> processor.process(bomb))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("40 megapixel");
  }

  @Test
  void magicBytesDetection() {
    assertThat(ImageProcessor.detect(TestImages.jpeg(10, 10)))
        .isEqualTo(ImageProcessor.Format.JPEG);
    assertThat(ImageProcessor.detect(TestImages.png(10, 10, false)))
        .isEqualTo(ImageProcessor.Format.PNG);
    byte[] webpHeader = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    assertThat(ImageProcessor.detect(webpHeader)).isEqualTo(ImageProcessor.Format.WEBP);
  }
}
