package com.white.handdam.poll.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneNotNull annotation) {
        this.fields = annotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        for (String fieldName : fields) {
            try {
                Object fieldValue = value.getClass().getMethod(fieldName).invoke(value);
                if (fieldValue != null) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
