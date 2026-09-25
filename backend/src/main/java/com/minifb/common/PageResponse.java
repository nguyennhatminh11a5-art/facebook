package com.minifb.common;

import java.util.List;
import java.util.function.Function;

/** Trang kết quả theo cursor (mục 5.1). */
public record PageResponse<T>(List<T> items, String nextCursor) {

  /**
   * Cắt danh sách đã lấy {@code size + 1} phần tử thành một trang.
   *
   * @param cursorOf tạo cursor từ phần tử cuối của trang khi còn trang sau
   */
  public static <T> PageResponse<T> of(List<T> fetched, int size, Function<T, String> cursorOf) {
    if (fetched.size() <= size) {
      return new PageResponse<>(List.copyOf(fetched), null);
    }
    List<T> page = List.copyOf(fetched.subList(0, size));
    return new PageResponse<>(page, cursorOf.apply(page.get(size - 1)));
  }

  public <R> PageResponse<R> map(Function<T, R> mapper) {
    return new PageResponse<>(items.stream().map(mapper).toList(), nextCursor);
  }
}
