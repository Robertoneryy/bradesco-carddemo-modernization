package com.carddemo.batch.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carddemo.batch.interest.copybook.AccountRecord;
import com.carddemo.batch.interest.copybook.CardXrefRecord;
import com.carddemo.batch.interest.copybook.DisclosureGroupRecord;
import com.carddemo.batch.interest.copybook.TranCatBalanceRecord;
import com.carddemo.batch.interest.copybook.TransactionRecord;
import com.carddemo.batch.interest.file.AccountFile;
import com.carddemo.batch.interest.file.CardXrefFile;
import com.carddemo.batch.interest.file.DisclosureGroupFile;
import com.carddemo.batch.interest.file.RecordFiles;
import com.carddemo.batch.interest.file.TranCatBalanceFile;
import com.carddemo.batch.interest.file.TransactionFile;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class InterestCalculationJobTest {

    private static final Path DATA = TestData.sampleDir();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2022-07-18T10:15:30.123Z"), ZoneOffset.UTC);

    private final ByteArrayOutputStream sysout = new ByteArrayOutputStream();

    @Test
    void sampleDataProducesOneInterestTransactionPerCategoryBalance() {
        List<AccountRecord> originalAccounts = RecordFiles.read(DATA.resolve("acctdata.txt"), AccountRecord::parse);
        Map<Long, String> cardByAccount = RecordFiles.read(DATA.resolve("cardxref.txt"), CardXrefRecord::parse)
                .stream().collect(Collectors.toMap(CardXrefRecord::acctId, CardXrefRecord::cardNum));
        AccountFile accounts = new AccountFile(originalAccounts);
        TransactionFile transactions = new TransactionFile();

        InterestCalculationJob.Summary summary = new InterestCalculationJob(
                "2022071800",
                TranCatBalanceFile.load(DATA.resolve("tcatbal.txt")),
                CardXrefFile.load(DATA.resolve("cardxref.txt")),
                DisclosureGroupFile.load(DATA.resolve("discgrp.txt")),
                accounts, transactions, CLOCK, printStream()).run();

        assertEquals(50, summary.recordsRead());
        assertEquals(50, summary.transactionsWritten());
        // All accounts except the last one get their balances posted (see lastAccountIsNotUpdated).
        assertEquals(49, summary.accountsUpdated());

        List<TransactionRecord> written = transactions.records();
        for (int i = 0; i < written.size(); i++) {
            TransactionRecord tx = written.get(i);
            long acctId = i + 1;
            assertEquals(String.format("2022071800%06d", i + 1), tx.tranId());
            assertEquals("01", tx.typeCd());
            assertEquals(5, tx.catCd());
            assertEquals("System", tx.source());
            assertEquals(String.format("Int. for a/c %011d", acctId), tx.desc().trim());
            // Every sample balance is zero, so the interest is zero too.
            assertEquals(new BigDecimal("0.00"), tx.amt());
            assertEquals(cardByAccount.get(acctId), tx.cardNum());
            assertEquals("2022-07-18-10.15.30.120000", tx.origTs());
            assertEquals(tx.origTs(), tx.procTs());
        }

        // Sample balances are zero, so posted accounts keep their balance.
        for (AccountRecord original : originalAccounts) {
            AccountRecord after = accounts.read(original.acctId()).orElseThrow();
            assertEquals(original.currBal(), after.currBal());
        }
        String log = sysout.toString(StandardCharsets.US_ASCII);
        assertTrue(log.startsWith("START OF EXECUTION OF PROGRAM CBACT04C"));
        assertTrue(log.contains("TRY WITH DEFAULT GROUP CODE"), "sample accounts have no group id");
        assertTrue(log.trim().endsWith("END OF EXECUTION OF PROGRAM CBACT04C"));
    }

    @Test
    void monthlyInterestIsBalanceTimesAnnualRateOver1200TruncatedToCents() {
        assertEquals(new BigDecimal("12.50"), InterestCalculationJob.monthlyInterest(bd("1000.00"), bd("15.00")));
        assertEquals(new BigDecimal("10.41"), InterestCalculationJob.monthlyInterest(bd("500.00"), bd("25.00")));
        assertEquals(new BigDecimal("-6.94"), InterestCalculationJob.monthlyInterest(bd("-333.33"), bd("25.00")));
        assertEquals(new BigDecimal("0.00"), InterestCalculationJob.monthlyInterest(bd("0.01"), bd("15.00")));
    }

    @Test
    void usesAccountGroupRateAndFallsBackToDefaultGroup() {
        Fixture fx = new Fixture()
                .account(1, "A000000000", "100.00")
                .account(2, "GOLD", "100.00")
                .account(3, "", "100.00")
                .balance(1, "01", 2, "1200.00")
                .balance(2, "01", 2, "1200.00")
                .balance(3, "01", 2, "1200.00")
                .rate("A000000000", "01", 2, "12.00")
                .rate("DEFAULT", "01", 2, "24.00");

        fx.run();

        assertEquals(List.of(bd("12.00"), bd("24.00"), bd("24.00")), fx.amounts());
        assertEquals(bd("112.00"), fx.balanceOf(1));
        assertEquals(bd("124.00"), fx.balanceOf(2));
    }

    @Test
    void zeroRateWritesNoTransactionButStillPostsAccount() {
        Fixture fx = new Fixture()
                .account(1, "ZEROAPR", "100.00", "30.00", "20.00")
                .account(2, "A", "0.00")
                .balance(1, "01", 1, "500.00")
                .balance(2, "01", 1, "500.00")
                .rate("ZEROAPR", "01", 1, "0.00")
                .rate("A", "01", 1, "12.00");

        fx.run();

        assertEquals(List.of(bd("5.00")), fx.amounts());
        AccountRecord account1 = fx.accounts.read(1).orElseThrow();
        assertEquals(bd("100.00"), account1.currBal());
        assertEquals(bd("0"), account1.currCycCredit());
        assertEquals(bd("0"), account1.currCycDebit());
    }

    @Test
    void interestOfAllCategoriesIsAddedToTheAccountBalance() {
        Fixture fx = new Fixture()
                .account(1, "A", "1000.00", "50.00", "25.00")
                .account(2, "A", "0.00")
                .balance(1, "01", 1, "1200.00")
                .balance(1, "01", 2, "-600.00")
                .balance(2, "01", 1, "0.00")
                .rate("A", "01", 1, "12.00")
                .rate("A", "01", 2, "6.00");

        fx.run();

        assertEquals(List.of(bd("12.00"), bd("-3.00"), bd("0.00")), fx.amounts());
        AccountRecord account1 = fx.accounts.read(1).orElseThrow();
        assertEquals(bd("1009.00"), account1.currBal());
        assertEquals(bd("0"), account1.currCycCredit());
        assertEquals(bd("0"), account1.currCycDebit());
    }

    @Test
    void lastAccountIsNotUpdated() {
        // Same as CBACT04C: the loop ends at end of file before the last account is posted.
        Fixture fx = new Fixture()
                .account(1, "A", "100.00", "50.00", "0.00")
                .balance(1, "01", 1, "1200.00")
                .rate("A", "01", 1, "12.00");

        InterestCalculationJob.Summary summary = fx.run();

        assertEquals(List.of(bd("12.00")), fx.amounts());
        assertEquals(0, summary.accountsUpdated());
        assertEquals(bd("100.00"), fx.balanceOf(1));
        assertEquals(bd("50.00"), fx.accounts.read(1).orElseThrow().currCycCredit());
    }

    @Test
    void balancesAreProcessedInKeyOrderWhateverTheInputOrder() {
        Fixture fx = new Fixture()
                .account(1, "A", "0.00")
                .account(2, "A", "0.00")
                .balance(2, "01", 1, "1200.00")
                .balance(1, "01", 2, "2400.00")
                .balance(1, "01", 1, "3600.00")
                .rate("A", "01", 1, "12.00")
                .rate("A", "01", 2, "12.00");

        fx.run();

        assertEquals(List.of(bd("36.00"), bd("24.00"), bd("12.00")), fx.amounts());
        assertEquals(List.of("2024013100000001", "2024013100000002", "2024013100000003"),
                fx.transactions.records().stream().map(TransactionRecord::tranId).toList());
    }

    @Test
    void missingAccountAbends() {
        Fixture fx = new Fixture()
                .balance(9, "01", 1, "100.00")
                .rate("DEFAULT", "01", 1, "12.00");

        AbendException abend = assertThrows(AbendException.class, fx::run);
        assertEquals("23", abend.fileStatus());
        assertTrue(sysout.toString(StandardCharsets.US_ASCII).contains("ACCOUNT NOT FOUND: 00000000009"));
    }

    @Test
    void missingCardCrossReferenceAbends() {
        Fixture fx = new Fixture()
                .account(1, "A", "0.00")
                .balance(1, "01", 1, "100.00")
                .rate("A", "01", 1, "12.00");
        fx.xrefs.clear();

        assertThrows(AbendException.class, fx::run);
        assertTrue(sysout.toString(StandardCharsets.US_ASCII).contains("ERROR READING XREF FILE"));
    }

    @Test
    void missingDefaultRateAbends() {
        Fixture fx = new Fixture()
                .account(1, "A", "0.00")
                .balance(1, "09", 9, "100.00");

        assertThrows(AbendException.class, fx::run);
        assertTrue(sysout.toString(StandardCharsets.US_ASCII).contains("ERROR READING DEFAULT DISCLOSURE GROUP"));
    }

    @Test
    void timestampUsesDb2Format() {
        InterestCalculationJob job = new Fixture().job();
        assertEquals("2022-07-18-10.15.30.120000", job.db2FormatTimestamp());
    }

    private PrintStream printStream() {
        return new PrintStream(sysout, true, StandardCharsets.US_ASCII);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** Builds small in-memory files for one run of the job. */
    private final class Fixture {
        final List<AccountRecord> accountList = new ArrayList<>();
        final List<CardXrefRecord> xrefs = new ArrayList<>();
        final List<TranCatBalanceRecord> balances = new ArrayList<>();
        final List<DisclosureGroupRecord> rates = new ArrayList<>();
        AccountFile accounts;
        final TransactionFile transactions = new TransactionFile();

        Fixture account(long id, String group, String balance) {
            return account(id, group, balance, "0.00", "0.00");
        }

        Fixture account(long id, String group, String balance, String cycCredit, String cycDebit) {
            accountList.add(new AccountRecord(id, "Y", bd(balance), bd("5000.00"), bd("1000.00"),
                    "2020-01-01", "2030-01-01", "2030-01-01", bd(cycCredit), bd(cycDebit), "12345", group, ""));
            xrefs.add(new CardXrefRecord(String.format("4000%012d", id), id, id, ""));
            return this;
        }

        Fixture balance(long acctId, String type, int cat, String amount) {
            balances.add(new TranCatBalanceRecord(acctId, type, cat, bd(amount), ""));
            return this;
        }

        Fixture rate(String group, String type, int cat, String rate) {
            rates.add(new DisclosureGroupRecord(new DisclosureGroupRecord.Key(group, type, cat), bd(rate), ""));
            return this;
        }

        InterestCalculationJob job() {
            accounts = new AccountFile(accountList);
            return new InterestCalculationJob("2024013100", new TranCatBalanceFile(balances),
                    new CardXrefFile(xrefs), new DisclosureGroupFile(rates), accounts, transactions,
                    CLOCK, printStream());
        }

        InterestCalculationJob.Summary run() {
            return job().run();
        }

        List<BigDecimal> amounts() {
            return transactions.records().stream().map(TransactionRecord::amt).toList();
        }

        BigDecimal balanceOf(long acctId) {
            return accounts.read(acctId).map(AccountRecord::currBal).orElseThrow();
        }
    }
}
