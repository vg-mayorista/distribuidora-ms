package com.distribuidora.notification.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Utility class to normalize Argentine phone numbers to E.164 format (+549XXXXXXXXXX).
 */
@Slf4j
public class ArgentinePhoneNormalizer {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D+");

    private ArgentinePhoneNormalizer() {
        // Utility class
    }

    /**
     * Normalizes an Argentine mobile phone number into E.164 format (+549XXXXXXXXXX).
     *
     * @param rawPhone Raw phone input string
     * @return Optional containing normalized phone (e.g., "+5491145678900"), or Optional.empty() if invalid
     */
    public static Optional<String> normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return Optional.empty();
        }

        // 1. Remove all non-digits
        String digits = NON_DIGIT.matcher(rawPhone).replaceAll("");
        if (digits.isEmpty()) {
            return Optional.empty();
        }

        // 2. Handle country code +54 / 54
        if (digits.startsWith("54")) {
            digits = digits.substring(2);
        }

        // 3. Remove leading 9 if present after 54 (e.g. 54911...) -> digits becomes 11...
        if (digits.startsWith("9")) {
            digits = digits.substring(1);
        }

        // 4. Remove leading 0 (national trunk prefix)
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        // 5. Remove "15" prefix if present after area code or at front
        // Standard AR mobile numbers without 0 and without 15 have exactly 10 digits (area code + line number).
        if (digits.length() == 12 && digits.substring(2).startsWith("15")) {
            // e.g., 11 15 45678900 -> area code 11 (2 digits), then 15, then 8 digits
            digits = digits.substring(0, 2) + digits.substring(4);
        } else if (digits.length() == 12 && digits.substring(3).startsWith("15")) {
            // e.g., 341 15 4567890 -> area code 341 (3 digits), then 15, then 7 digits
            digits = digits.substring(0, 3) + digits.substring(5);
        } else if (digits.length() == 12 && digits.substring(4).startsWith("15")) {
            // e.g., 2994 15 456789 -> area code 2994 (4 digits), then 15, then 6 digits
            digits = digits.substring(0, 4) + digits.substring(6);
        } else if ((digits.length() == 11 || digits.length() == 10) && digits.startsWith("15")) {
            // e.g. 15 45678900 (missing area code, assuming Buenos Aires 11)
            digits = "11" + digits.substring(2);
        }

        // 6. Valid AR mobile numbers must have exactly 10 digits (area code + subscriber number)
        if (digits.length() != 10) {
            log.debug("Phone normalization failed for input '{}' (processed digits '{}', length {})",
                    rawPhone, digits, digits.length());
            return Optional.empty();
        }

        return Optional.of("+549" + digits);
    }
}
