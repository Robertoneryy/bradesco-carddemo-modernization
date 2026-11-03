package com.carddemo.batch.interest.copybook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PicTest {

    @ParameterizedTest
    @CsvSource({
        "0000000000{, 0.00",
        "0000001940{, 194.00",
        "0000000104A, 10.41",
        "0000000069M, -6.94",
        "0000000001}, -0.10",
        "00150{, 15.00",
        "99999I, 9999.99",
        "0000000123, 1.23",
    })
    void parsesZonedDecimalWithOverpunchedSign(String text, String expected) {
        assertEquals(new BigDecimal(expected), Pic.parseZoned(text, 2));
    }

    @ParameterizedTest
    @CsvSource({
        "0.00, 0000000000{",
        "12.50, 0000000125{",
        "10.41, 0000000104A",
        "-6.94, 0000000069M",
        "-250.75, 0000002507N",
    })
    void formatsZonedDecimalWithOverpunchedSign(String value, String expected) {
        assertEquals(expected, Pic.zoned(new BigDecimal(value), 9, 2));
    }

    @Test
    void fitTruncatesDecimalsInsteadOfRounding() {
        assertEquals(new BigDecimal("10.41"), Pic.fit(new BigDecimal("10.41666"), 9, 2));
        assertEquals(new BigDecimal("-6.94"), Pic.fit(new BigDecimal("-6.944375"), 9, 2));
    }

    @Test
    void fitDropsHighOrderDigitsThatDoNotFit() {
        assertEquals(new BigDecimal("333324999.91"), Pic.fit(new BigDecimal("8333324999.91"), 9, 2));
        assertEquals(new BigDecimal("-1.00"), Pic.fit(new BigDecimal("-1000000001.00"), 9, 2));
    }

    @Test
    void negativeZeroIsStoredAsPositiveZero() {
        assertEquals("0000000000{", Pic.zoned(Pic.fit(new BigDecimal("-0.00125"), 9, 2), 9, 2));
    }

    @Test
    void alphaPadsAndTruncates() {
        assertEquals("DEFAULT   ", Pic.alpha("DEFAULT", 10));
        assertEquals("ABC", Pic.alpha("ABCDEF", 3));
    }

    @Test
    void unsignedPadsWithZeros() {
        assertEquals("00000000042", Pic.unsigned(42, 11));
        assertEquals("000000", Pic.unsigned(1_000_000, 6));
    }
}
