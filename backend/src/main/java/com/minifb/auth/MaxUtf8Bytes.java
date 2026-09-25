package com.minifb.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/** Giới hạn số byte UTF-8 (BCrypt bỏ qua phần sau 72 byte). */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxUtf8Bytes.Validator.class)
public @interface MaxUtf8Bytes {

  int value();

  String message() default "Quá dài (tối đa {value} byte UTF-8)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int max;

    @Override
    public void initialize(MaxUtf8Bytes annotation) {
      this.max = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
  }
}
