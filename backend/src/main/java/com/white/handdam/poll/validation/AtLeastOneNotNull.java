package com.white.handdam.poll.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Constraint(validatedBy = AtLeastOneNotNullValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneNotNull {
    String message() default "최소 하나의 필드를 입력해야 합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] fields();
}
