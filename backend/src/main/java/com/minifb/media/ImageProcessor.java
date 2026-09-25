package com.minifb.media;

import com.minifb.common.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra và mã hoá lại ảnh upload (CLAUDE.md mục 5.4): nhận diện bằng magic bytes, chặn ảnh >
 * 40MP trước khi giải nén, xoay theo EXIF, bỏ toàn bộ metadata, resize, tạo thumbnail.
 */
@Component
public class ImageProcessor {

  public static final long MAX_BYTES = 5L * 1024 * 1024;
  static final long MAX_PIXELS = 40_000_000L;
  static final int MAX_SIDE = 2048;
  static final int THUMB_SIDE = 640;
  private static final double JPEG_QUALITY = 0.85;

  enum Format {
    JPEG("jpeg"),
    PNG("png"),
    WEBP("webp");

    private final String readerName;

    Format(String readerName) {
      this.readerName = readerName;
    }
  }

  public ProcessedImage process(byte[] data) {
    if (data == null || data.length == 0) {
      throw invalid();
    }
    if (data.length > MAX_BYTES) {
      throw new BadRequestException("Ảnh vượt quá 5 MB");
    }
    Format format = detect(data);
    BufferedImage oriented = decode(data, format);

    boolean alpha = oriented.getColorModel().hasAlpha();
    String extension = alpha ? "png" : "jpg";
    BufferedImage main = resize(oriented, MAX_SIDE, alpha);
    BufferedImage thumb = resize(oriented, THUMB_SIDE, alpha);
    return new ProcessedImage(
        encode(main, extension),
        encode(thumb, extension),
        extension,
        main.getWidth(),
        main.getHeight());
  }

  /** Nhận diện định dạng bằng magic bytes, không tin Content-Type hay đuôi file. */
  static Format detect(byte[] d) {
    if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
      return Format.JPEG;
    }
    byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    if (startsWith(d, png, 0)) {
      return Format.PNG;
    }
    if (d.length >= 12
        && startsWith(d, new byte[] {'R', 'I', 'F', 'F'}, 0)
        && startsWith(d, new byte[] {'W', 'E', 'B', 'P'}, 8)) {
      return Format.WEBP;
    }
    throw invalid();
  }

  private static BufferedImage decode(byte[] data, Format format) {
    checkDimensions(data, format);
    try {
      // Thumbnailator đọc EXIF orientation và xoay ảnh; ảnh ra là BufferedImage không metadata
      BufferedImage image =
          Thumbnails.of(new ByteArrayInputStream(data))
              .scale(1.0)
              .useExifOrientation(true)
              .asBufferedImage();
      if (image == null) {
        throw invalid();
      }
      return image;
    } catch (IOException | RuntimeException e) {
      if (e instanceof BadRequestException bad) {
        throw bad;
      }
      throw invalid();
    }
  }

  /** Đọc kích thước từ header, chưa giải nén điểm ảnh (chống decompression bomb). */
  private static void checkDimensions(byte[] data, Format format) {
    try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.readerName);
      if (!readers.hasNext()) {
        throw invalid();
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(in, true, true);
        long width = reader.getWidth(0);
        long height = reader.getHeight(0);
        if (width <= 0 || height <= 0) {
          throw invalid();
        }
        if (width * height > MAX_PIXELS) {
          throw new BadRequestException("Ảnh quá lớn (tối đa 40 megapixel)");
        }
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw invalid();
    }
  }

  private static BufferedImage resize(BufferedImage source, int maxSide, boolean alpha) {
    int longSide = Math.max(source.getWidth(), source.getHeight());
    try {
      var builder = Thumbnails.of(source);
      if (longSide > maxSide) {
        builder.size(maxSide, maxSide).keepAspectRatio(true);
      } else {
        builder.scale(1.0);
      }
      builder.imageType(alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
      return builder.asBufferedImage();
    } catch (IOException e) {
      throw invalid();
    }
  }

  private static byte[] encode(BufferedImage image, String extension) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      var builder = Thumbnails.of(image).scale(1.0).outputFormat(extension);
      if ("jpg".equals(extension)) {
        builder.outputQuality(JPEG_QUALITY);
      }
      builder.toOutputStream(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Không mã hoá được ảnh", e);
    }
  }

  private static boolean startsWith(byte[] data, byte[] prefix, int offset) {
    if (data.length < offset + prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (data[offset + i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private static BadRequestException invalid() {
    return new BadRequestException("File không phải ảnh JPEG/PNG/WebP hợp lệ");
  }
}
