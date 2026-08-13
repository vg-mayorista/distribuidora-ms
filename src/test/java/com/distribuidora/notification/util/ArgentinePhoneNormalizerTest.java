package com.distribuidora.notification.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ArgentinePhoneNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "1145678900, +5491145678900",
            "011 15-4567-8900, +5491145678900",
            "+54 9 11 4567-8900, +5491145678900",
            "5491145678900, +5491145678900",
            "341 555-1234, +5493415551234",
            "0341 15-555-1234, +5493415551234",
            "1545678900, +5491145678900"
    })
    void normalizesValidArgentineNumbers(String input, String expected) {
        Optional<String> result = ArgentinePhoneNormalizer.normalize(input);
        assertThat(result).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "123",
            "abc",
            "1234567890123456"
    })
    void returnsEmptyForInvalidNumbers(String input) {
        Optional<String> result = ArgentinePhoneNormalizer.normalize(input);
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyForNull() {
        assertThat(ArgentinePhoneNormalizer.normalize(null)).isEmpty();
    }
}
