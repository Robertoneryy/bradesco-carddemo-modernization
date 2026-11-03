package com.carddemo.batch.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.carddemo.batch.interest.copybook.AccountRecord;
import com.carddemo.batch.interest.copybook.TransactionRecord;
import com.carddemo.batch.interest.file.AccountFile;
import com.carddemo.batch.interest.file.CardXrefFile;
import com.carddemo.batch.interest.file.DisclosureGroupFile;
import com.carddemo.batch.interest.file.TranCatBalanceFile;
import com.carddemo.batch.interest.file.TransactionFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs each scenario under src/test/resources/scenarios and compares the Java output with the
 * output of the original COBOL program, recorded by cobol-reference/generate-expected.sh.
 * Inputs not supplied by a scenario come from app/data/ASCII.
 */
class CobolEquivalenceTest {

    /** Matches the fixed timestamp written into the expected files by generate-expected.sh. */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("0001-01-01T00:00:00Z"), ZoneOffset.UTC);

    static Stream<Path> scenarios() throws IOException, URISyntaxException {
        Path root = Path.of(CobolEquivalenceTest.class.getResource("/scenarios").toURI());
        return Files.list(root).filter(Files::isDirectory).sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void javaOutputMatchesCobolOutput(Path scenario) throws IOException {
        String parmDate = Files.readString(scenario.resolve("parm.txt")).trim();
        Path expected = scenario.resolve("expected");
        String result = Files.readString(expected.resolve("result.txt")).trim();

        AccountFile accounts = AccountFile.load(input(scenario, "acctdata.txt"));
        TransactionFile transactions = new TransactionFile();
        ByteArrayOutputStream sysout = new ByteArrayOutputStream();
        InterestCalculationJob job = new InterestCalculationJob(
                parmDate,
                TranCatBalanceFile.load(input(scenario, "tcatbal.txt")),
                CardXrefFile.load(input(scenario, "cardxref.txt")),
                DisclosureGroupFile.load(input(scenario, "discgrp.txt")),
                accounts,
                transactions,
                FIXED_CLOCK,
                new PrintStream(sysout, true, StandardCharsets.US_ASCII));

        if ("ABENDED".equals(result)) {
            assertThrows(AbendException.class, job::run);
        } else {
            job.run();
            assertEquals(lines(expected.resolve("systran.txt")),
                    trimmed(transactions.records().stream().map(TransactionRecord::toRecord).toList()),
                    "transaction file");
            assertEquals(lines(expected.resolve("acctdata.txt")),
                    trimmed(accounts.records().stream().map(AccountRecord::toRecord).toList()),
                    "account file");
        }
        assertEquals(lines(expected.resolve("sysout.txt")),
                trimmed(sysout.toString(StandardCharsets.US_ASCII).lines().toList()),
                "program messages");
    }

    private static Path input(Path scenario, String name) {
        Path override = scenario.resolve("input").resolve(name);
        return Files.exists(override) ? override : TestData.sampleDir().resolve(name);
    }

    private static List<String> lines(Path file) throws IOException {
        return trimmed(Files.readAllLines(file, StandardCharsets.US_ASCII));
    }

    private static List<String> trimmed(List<String> lines) {
        return lines.stream().map(String::stripTrailing).toList();
    }
}
