package kz.iitu.springlab.lab6.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoDigitsValidator implements ConstraintValidator<NoDigits, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank checks missing values; this constraint checks Unicode digits.
        return value == null || value.codePoints().noneMatch(Character::isDigit);
    }
}
