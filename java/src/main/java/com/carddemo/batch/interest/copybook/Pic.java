package com.carddemo.batch.interest.copybook;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Helpers that reproduce how COBOL PIC clauses store values in fixed-width records.
 *
 * <ul>
 *   <li>{@code PIC X(n)}: text, padded with spaces or cut to n characters.</li>
 *   <li>{@code PIC 9(n)}: unsigned whole number, zero padded.</li>
 *   <li>{@code PIC S9(i)V9(d)}: signed "zoned decimal" number. The sign is folded into the
 *       last digit ("overpunch"): {@code {,A..I} = +0..+9} and {@code },J..R} = -0..-9}.
 *       The decimal point is implied, not stored.</li>
 * </ul>
 */
public final class Pic {

    private static final String POSITIVE_OVERPUNCH = "{ABCDEFGHI";
    private static final String NEGATIVE_OVERPUNCH = "}JKLMNOPQR";
    private static final String ASCII_NEGATIVE_OVERPUNCH = "pqrstuvwxy";

    private Pic() {
    }

    public static String field(String record, int offset, int length) {
        if (record.length() >= offset + length) {
            return record.substring(offset, offset + length);
        }
        String available = offset < record.length() ? record.substring(offset) : "";
        return alpha(available, length);
    }

    public static String alpha(String value, int length) {
        String text = value == null ? "" : value;
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }

    public static long parseUnsigned(String digits) {
        String trimmed = digits.trim();
        return trimmed.isEmpty() ? 0L : Long.parseLong(trimmed);
    }

    /** Formats an unsigned number; like a COBOL MOVE, high-order digits that do not fit are dropped. */
    public static String unsigned(long value, int digits) {
        String text = Long.toString(Math.abs(value));
        if (text.length() > digits) {
            text = text.substring(text.length() - digits);
        }
        return "0".repeat(digits - text.length()) + text;
    }

    public static BigDecimal parseZoned(String text, int scale) {
        if (text.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        char last = text.charAt(text.length() - 1);
        String body = text.substring(0, text.length() - 1);
        int digit;
        boolean negative;
        if (Character.isDigit(last)) {
            digit = last - '0';
            negative = false;
        } else if (POSITIVE_OVERPUNCH.indexOf(last) >= 0) {
            digit = POSITIVE_OVERPUNCH.indexOf(last);
            negative = false;
        } else if (NEGATIVE_OVERPUNCH.indexOf(last) >= 0) {
            digit = NEGATIVE_OVERPUNCH.indexOf(last);
            negative = true;
        } else if (ASCII_NEGATIVE_OVERPUNCH.indexOf(last) >= 0) {
            digit = ASCII_NEGATIVE_OVERPUNCH.indexOf(last);
            negative = true;
        } else {
            throw new IllegalArgumentException("Invalid zoned decimal sign in '" + text + "'");
        }
        String digits = body.replace(' ', '0') + digit;
        BigInteger unscaled = new BigInteger(digits);
        return new BigDecimal(negative ? unscaled.negate() : unscaled, scale);
    }

    public static String zoned(BigDecimal value, int integerDigits, int scale) {
        BigDecimal fitted = fit(value, integerDigits, scale);
        String digits = fitted.unscaledValue().abs().toString();
        int width = integerDigits + scale;
        digits = "0".repeat(width - digits.length()) + digits;
        int lastDigit = digits.charAt(width - 1) - '0';
        char sign = fitted.signum() < 0
                ? NEGATIVE_OVERPUNCH.charAt(lastDigit)
                : POSITIVE_OVERPUNCH.charAt(lastDigit);
        return digits.substring(0, width - 1) + sign;
    }

    /**
     * Stores a value in a {@code PIC S9(integerDigits)V9(scale)} field the way COBOL does
     * without ROUNDED or ON SIZE ERROR: extra decimals are cut off (not rounded) and
     * high-order digits that do not fit are lost.
     */
    public static BigDecimal fit(BigDecimal value, int integerDigits, int scale) {
        BigDecimal truncated = value.setScale(scale, RoundingMode.DOWN);
        BigInteger limit = BigInteger.TEN.pow(integerDigits + scale);
        return new BigDecimal(truncated.unscaledValue().remainder(limit), scale);
    }
}
