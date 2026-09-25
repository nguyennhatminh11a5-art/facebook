package com.minifb.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new DummyController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void notFoundBecomesProblemDetail() throws Exception {
    mvc.perform(get("/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Không tìm thấy"));
  }

  @Test
  void forbiddenAndConflictMapToStatus() throws Exception {
    mvc.perform(get("/forbidden")).andExpect(status().isForbidden());
    mvc.perform(get("/conflict")).andExpect(status().isConflict());
  }

  @Test
  void tooManyRequestsHasRetryAfter() throws Exception {
    mvc.perform(get("/too-many"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "42"));
  }

  @Test
  void validationErrorListsFields() throws Exception {
    mvc.perform(
            post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
  }

  @Test
  void unexpectedErrorHidesDetails() throws Exception {
    mvc.perform(get("/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("Lỗi hệ thống"))
        .andExpect(content().string(not(containsString("secret-internal"))));
  }

  record NameRequest(@NotBlank String name) {}

  @RestController
  static class DummyController {

    @GetMapping("/not-found")
    void notFound() {
      throw new NotFoundException("Không tìm thấy");
    }

    @GetMapping("/forbidden")
    void forbidden() {
      throw new ForbiddenException("Không được phép");
    }

    @GetMapping("/conflict")
    void conflict() {
      throw new ConflictException("Trùng");
    }

    @GetMapping("/too-many")
    void tooMany() {
      throw new TooManyRequestsException(42);
    }

    @PostMapping("/validate")
    void validate(@Valid @RequestBody NameRequest request) {}

    @GetMapping("/boom")
    void boom() {
      throw new IllegalStateException("secret-internal");
    }
  }
}
