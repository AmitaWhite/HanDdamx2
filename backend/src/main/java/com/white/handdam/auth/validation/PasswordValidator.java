package com.white.handdam.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern TRIPLE = Pattern.compile("(.)\\1{2,}");

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null ||
            password.length() < MIN_LENGTH ||
            password.length() > MAX_LENGTH) {
            return false;
        }

        // 동일 문자 3회 이상 연속 사용 불가
        if (TRIPLE.matcher(password).find()) {
            return false;
        }

        // 공백 불가
        if (password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }

        int count = 0;
        if (UPPER.matcher(password).find()) count++;
        if (LOWER.matcher(password).find()) count++;
        if (DIGIT.matcher(password).find()) count++;
        if (SPECIAL.matcher(password).find()) count++;

        return count >= 3;
    }
}
