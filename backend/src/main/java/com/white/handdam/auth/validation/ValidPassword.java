package com.white.handdam.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordValidator.class) // 어노테이션 적용 위치
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME) // 어노테이션 적용 범위
public @interface ValidPassword {

    String message () default "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자 중 3개 이상을 포함하여 8자 이상이어야 하며, 동일한 문자를 3회 이상 연속 사용할 수 없습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
