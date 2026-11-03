package com.carddemo.batch.interest.copybook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.carddemo.batch.interest.file.RecordFiles;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Every record in app/data/ASCII must map to its Java record and back without losing a byte. */
class CopybookRecordsTest {

    private static final Path DATA = Path.of(System.getProperty("carddemo.data.dir", "../app/data/ASCII"));

    @Test
    void tranCatBalanceRecordsRoundTrip() {
        assertRoundTrip("tcatbal.txt", TranCatBalanceRecord.LENGTH, TranCatBalanceRecord::parse,
                TranCatBalanceRecord::toRecord);
    }

    @Test
    void cardXrefRecordsRoundTrip() {
        assertRoundTrip("cardxref.txt", CardXrefRecord.LENGTH, CardXrefRecord::parse, CardXrefRecord::toRecord);
    }

    @Test
    void disclosureGroupRecordsRoundTrip() {
        assertRoundTrip("discgrp.txt", DisclosureGroupRecord.LENGTH, DisclosureGroupRecord::parse,
                DisclosureGroupRecord::toRecord);
    }

    @Test
    void accountRecordsRoundTrip() {
        assertRoundTrip("acctdata.txt", AccountRecord.LENGTH, AccountRecord::parse, AccountRecord::toRecord);
    }

    @Test
    void transactionRecordsRoundTrip() {
        // dailytran.txt uses the same 350-byte layout as CVTRA05Y.
        assertRoundTrip("dailytran.txt", TransactionRecord.LENGTH, TransactionRecord::parse,
                TransactionRecord::toRecord);
    }

    @Test
    void parsesFieldsOfFirstSampleAccount() {
        AccountRecord account = RecordFiles.read(DATA.resolve("acctdata.txt"), AccountRecord::parse).get(0);
        assertEquals(1L, account.acctId());
        assertEquals("Y", account.activeStatus());
        assertEquals(new BigDecimal("194.00"), account.currBal());
        assertEquals(new BigDecimal("2020.00"), account.creditLimit());
        assertEquals(new BigDecimal("1020.00"), account.cashCreditLimit());
        assertEquals("2014-11-20", account.openDate());
        assertEquals("A000000000", account.addrZip());
        assertEquals("          ", account.groupId());
    }

    @Test
    void parsesFieldsOfFirstDisclosureGroup() {
        DisclosureGroupRecord group = RecordFiles.read(DATA.resolve("discgrp.txt"), DisclosureGroupRecord::parse)
                .get(0);
        assertEquals(new DisclosureGroupRecord.Key("A000000000", "01", 1), group.key());
        assertEquals(new BigDecimal("15.00"), group.intRate());
    }

    private static <T> void assertRoundTrip(String file, int length, Function<String, T> parser,
            Function<T, String> formatter) {
        List<String> lines = RecordFiles.read(DATA.resolve(file), Function.identity());
        assertFalse(lines.isEmpty(), file + " has no records");
        for (String line : lines) {
            String expected = Pic.alpha(line, length);
            assertEquals(expected, formatter.apply(parser.apply(line)), "Round trip of " + file);
        }
    }
}
