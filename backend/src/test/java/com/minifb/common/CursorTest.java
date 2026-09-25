package com.minifb.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorTest {

  @Test
  void timeIdRoundTrip() {
    Instant at = Instant.parse("2026-01-02T03:04:05.123456Z");
    Cursor.TimeId decoded = Cursor.decodeTimeId(Cursor.encode(at, 42));
    assertThat(decoded.createdAt()).isEqualTo(at);
    assertThat(decoded.id()).isEqualTo(42);
  }

  @Test
  void idRoundTrip() {
    assertThat(Cursor.decodeId(Cursor.encode(7L))).isEqualTo(7);
  }

  @Test
  void malformedCursorIsBadRequest() {
    assertThatThrownBy(() -> Cursor.decodeTimeId("%%%")).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> Cursor.decodeTimeId(Cursor.encode(5L)))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> Cursor.decodeId("YWJj")).isInstanceOf(BadRequestException.class);
  }

  @Test
  void sizeDefaultsAndBounds() {
    assertThat(Cursor.size(null)).isEqualTo(10);
    assertThat(Cursor.size(50)).isEqualTo(50);
    assertThatThrownBy(() -> Cursor.size(0)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> Cursor.size(51)).isInstanceOf(BadRequestException.class);
  }

  @Test
  void pageResponseCutsExtraItem() {
    PageResponse<Integer> page = PageResponse.of(List.of(1, 2, 3), 2, i -> "c" + i);
    assertThat(page.items()).containsExactly(1, 2);
    assertThat(page.nextCursor()).isEqualTo("c2");
    assertThat(PageResponse.of(List.of(1, 2), 2, i -> "c" + i).nextCursor()).isNull();
  }
}
