package kz.iitu.springlab.lab6;

import kz.iitu.springlab.lab6.validation.NoDigitsValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NoDigitsValidatorTests {

    private final NoDigitsValidator validator = new NoDigitsValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Author2", "Автор٣", "Author９", "Author𝟙"})
    void rejectsAsciiAndUnicodeDigits(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Valid Author", "Абай Құнанбайұлы"})
    void acceptsNamesAndLeavesMissingValuesToNotBlank(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    void recognizesSupplementaryCodePoints() {
        assertThat("𝟙".length()).isEqualTo(2);
        assertThat(validator.isValid("Author𝟙", null)).isFalse();
    }
}
