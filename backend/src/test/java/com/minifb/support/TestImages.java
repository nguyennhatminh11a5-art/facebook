package com.minifb.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/** Tạo ảnh test trong bộ nhớ. */
public final class TestImages {

  private TestImages() {}

  public static byte[] jpeg(int width, int height) {
    return encode(image(width, height, BufferedImage.TYPE_INT_RGB), "jpg");
  }

  public static byte[] png(int width, int height, boolean alpha) {
    return encode(
        image(width, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB),
        "png");
  }

  /** JPEG có đoạn APP1 Exif: Orientation = 6 (xoay 90°) và GPS (GPSLatitudeRef = N). */
  public static byte[] jpegWithExifGps(int width, int height) {
    byte[] jpeg = jpeg(width, height);
    // Chèn sau đoạn APP0 (JFIF) đứng ngay sau SOI
    int app0Length = ((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF);
    int insertAt = 4 + app0Length;

    ByteBuffer tiff = ByteBuffer.allocate(56);
    tiff.put(new byte[] {'M', 'M', 0, 42}).putInt(8);
    // IFD0: 2 mục
    tiff.putShort((short) 2);
    tiff.putShort((short) 0x0112)
        .putShort((short) 3)
        .putInt(1)
        .putShort((short) 6)
        .putShort((short) 0);
    tiff.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(38);
    tiff.putInt(0);
    // GPS IFD tại offset 38: GPSLatitudeRef = "N"
    tiff.putShort((short) 1);
    tiff.putShort((short) 0x0001).putShort((short) 2).putInt(2).put(new byte[] {'N', 0, 0, 0});
    tiff.putInt(0);

    byte[] exifHeader = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
    int segmentLength = 2 + exifHeader.length + tiff.position();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(jpeg, 0, insertAt);
    out.write(0xFF);
    out.write(0xE1);
    out.write(segmentLength >> 8);
    out.write(segmentLength & 0xFF);
    out.writeBytes(exifHeader);
    out.write(tiff.array(), 0, tiff.position());
    out.write(jpeg, insertAt, jpeg.length - insertAt);
    return out.toByteArray();
  }

  /** PNG chỉ có header khai báo kích thước rất lớn (không có dữ liệu điểm ảnh hợp lệ). */
  public static byte[] pngHeaderOnly(int width, int height) {
    ByteBuffer ihdr = ByteBuffer.allocate(13);
    ihdr.putInt(width)
        .putInt(height)
        .put((byte) 8)
        .put((byte) 2)
        .put((byte) 0)
        .put((byte) 0)
        .put((byte) 0);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    writeChunk(out, "IHDR", ihdr.array());
    writeChunk(out, "IDAT", new byte[] {0x78, (byte) 0x9C, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01});
    writeChunk(out, "IEND", new byte[0]);
    return out.toByteArray();
  }

  public static BufferedImage read(byte[] data) {
    try {
      return ImageIO.read(new java.io.ByteArrayInputStream(data));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Có đoạn APP1 (Exif/XMP) trong JPEG không. */
  public static boolean hasApp1Segment(byte[] jpeg) {
    for (int i = 2; i + 1 < jpeg.length; ) {
      if ((jpeg[i] & 0xFF) != 0xFF) {
        return false;
      }
      int marker = jpeg[i + 1] & 0xFF;
      if (marker == 0xE1) {
        return true;
      }
      if (marker == 0xDA) {
        return false; // bắt đầu dữ liệu ảnh
      }
      int length = ((jpeg[i + 2] & 0xFF) << 8) | (jpeg[i + 3] & 0xFF);
      i += 2 + length;
    }
    return false;
  }

  private static BufferedImage image(int width, int height, int type) {
    BufferedImage image = new BufferedImage(width, height, type);
    Graphics2D g = image.createGraphics();
    g.setColor(Color.ORANGE);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.BLUE);
    g.fillRect(0, 0, width / 2, height / 2);
    g.dispose();
    return image;
  }

  private static byte[] encode(BufferedImage image, String format) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, format, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) {
    byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
    out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
    out.writeBytes(typeBytes);
    out.writeBytes(data);
    CRC32 crc = new CRC32();
    crc.update(typeBytes);
    crc.update(data);
    out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
  }
}
