package com.minifb.media;

/**
 * Ảnh đã mã hoá lại (không còn metadata).
 *
 * @param extension {@code jpg} hoặc {@code png}
 */
public record ProcessedImage(
    byte[] main, byte[] thumbnail, String extension, int width, int height) {

  @Override
  public String toString() {
    return "ProcessedImage[" + extension + " " + width + "x" + height + "]";
  }
}
