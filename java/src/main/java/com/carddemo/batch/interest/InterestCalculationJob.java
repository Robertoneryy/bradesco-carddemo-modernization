package com.carddemo.batch.interest;

import com.carddemo.batch.interest.copybook.AccountRecord;
import com.carddemo.batch.interest.copybook.CardXrefRecord;
import com.carddemo.batch.interest.copybook.DisclosureGroupRecord;
import com.carddemo.batch.interest.copybook.Pic;
import com.carddemo.batch.interest.copybook.TranCatBalanceRecord;
import com.carddemo.batch.interest.copybook.TransactionRecord;
import com.carddemo.batch.interest.file.AccountFile;
import com.carddemo.batch.interest.file.CardXrefFile;
import com.carddemo.batch.interest.file.DisclosureGroupFile;
import com.carddemo.batch.interest.file.TranCatBalanceFile;
import com.carddemo.batch.interest.file.TransactionFile;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * Java version of the COBOL batch program CBACT04C (interest calculator).
 *
 * <p>For every transaction category balance, it looks up the annual interest rate for the
 * account's disclosure group (falling back to the DEFAULT group), computes one month of
 * interest, writes an interest transaction, and adds the total interest to the account's
 * current balance. Method names follow the COBOL paragraph they replace.
 */
public final class InterestCalculationJob {

    static final String DEFAULT_GROUP_ID = "DEFAULT";
    static final String INTEREST_TRAN_TYPE_CD = "01";
    static final int INTEREST_TRAN_CAT_CD = 5;
    static final String INTEREST_TRAN_SOURCE = "System";
    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);
    private static final DateTimeFormatter DB2_TS_PREFIX = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.");

    private final String parmDate;
    private final Clock clock;
    private final PrintStream sysout;

    private final TranCatBalanceFile tranCatBalanceFile;
    private final CardXrefFile xrefFile;
    private final DisclosureGroupFile disclosureGroupFile;
    private final AccountFile accountFile;
    private final TransactionFile transactionFile;

    private TranCatBalanceRecord tranCatBal;
    private AccountRecord account;
    private CardXrefRecord xref;
    private DisclosureGroupRecord disGroup;

    private Long lastAcctNum;
    private boolean firstTime = true;
    private BigDecimal monthlyInt = BigDecimal.ZERO;
    private BigDecimal totalInt = BigDecimal.ZERO;
    private long recordCount;
    private int tranIdSuffix;
    private int accountsUpdated;

    public InterestCalculationJob(
            String parmDate,
            TranCatBalanceFile tranCatBalanceFile,
            CardXrefFile xrefFile,
            DisclosureGroupFile disclosureGroupFile,
            AccountFile accountFile,
            TransactionFile transactionFile,
            Clock clock,
            PrintStream sysout) {
        this.parmDate = Objects.requireNonNull(parmDate, "parmDate");
        this.tranCatBalanceFile = tranCatBalanceFile;
        this.xrefFile = xrefFile;
        this.disclosureGroupFile = disclosureGroupFile;
        this.accountFile = accountFile;
        this.transactionFile = transactionFile;
        this.clock = clock;
        this.sysout = sysout;
    }

    public record Summary(long recordsRead, int transactionsWritten, int accountsUpdated) {
    }

    /** PROCEDURE DIVISION main line. */
    public Summary run() {
        sysout.println("START OF EXECUTION OF PROGRAM CBACT04C");

        boolean endOfFile = false;
        while (!endOfFile) {
            Optional<TranCatBalanceRecord> next = readNextTranCatBalance();
            if (next.isEmpty()) {
                // As in CBACT04C, the loop stops right at end of file. The COBOL branch that
                // would post the last account's interest (PERFORM 1050-UPDATE-ACCOUNT after EOF)
                // is never reached, so the last account's balance is not updated.
                endOfFile = true;
                continue;
            }
            tranCatBal = next.get();
            recordCount++;
            sysout.println(tranCatBal.toRecord());

            if (!Long.valueOf(tranCatBal.acctId()).equals(lastAcctNum)) {
                if (!firstTime) {
                    updateAccount();
                } else {
                    firstTime = false;
                }
                totalInt = BigDecimal.ZERO;
                lastAcctNum = tranCatBal.acctId();
                getAccountData(tranCatBal.acctId());
                getXrefData(tranCatBal.acctId());
            }

            getInterestRate(new DisclosureGroupRecord.Key(
                    account.groupId(), tranCatBal.typeCd(), tranCatBal.catCd()));
            if (disGroup.intRate().signum() != 0) {
                computeInterest();
                computeFees();
            }
        }

        sysout.println("END OF EXECUTION OF PROGRAM CBACT04C");
        return new Summary(recordCount, transactionFile.records().size(), accountsUpdated);
    }

    /** 1000-TCATBALF-GET-NEXT. */
    Optional<TranCatBalanceRecord> readNextTranCatBalance() {
        return tranCatBalanceFile.readNext();
    }

    /** 1050-UPDATE-ACCOUNT: add the month's interest to the balance and reset the cycle totals. */
    void updateAccount() {
        AccountRecord updated = account.withBalances(
                Pic.fit(account.currBal().add(totalInt), 10, 2),
                BigDecimal.ZERO,
                BigDecimal.ZERO);
        if (!accountFile.rewrite(updated)) {
            abend("ERROR RE-WRITING ACCOUNT FILE", "23");
        }
        account = updated;
        accountsUpdated++;
    }

    /** 1100-GET-ACCT-DATA. */
    void getAccountData(long acctId) {
        Optional<AccountRecord> found = accountFile.read(acctId);
        if (found.isEmpty()) {
            sysout.println("ACCOUNT NOT FOUND: " + Pic.unsigned(acctId, 11));
            abend("ERROR READING ACCOUNT FILE", "23");
        }
        account = found.get();
    }

    /** 1110-GET-XREF-DATA: find the card linked to the account (alternate key XREF-ACCT-ID). */
    void getXrefData(long acctId) {
        Optional<CardXrefRecord> found = xrefFile.readByAcctId(acctId);
        if (found.isEmpty()) {
            sysout.println("ACCOUNT NOT FOUND: " + Pic.unsigned(acctId, 11));
            abend("ERROR READING XREF FILE", "23");
        }
        xref = found.get();
    }

    /** 1200-GET-INTEREST-RATE: rate for the account's group, or the DEFAULT group if missing. */
    void getInterestRate(DisclosureGroupRecord.Key key) {
        Optional<DisclosureGroupRecord> found = disclosureGroupFile.read(key);
        if (found.isPresent()) {
            disGroup = found.get();
            return;
        }
        sysout.println("DISCLOSURE GROUP RECORD MISSING");
        sysout.println("TRY WITH DEFAULT GROUP CODE");
        getDefaultInterestRate(new DisclosureGroupRecord.Key(
                DEFAULT_GROUP_ID, key.tranTypeCd(), key.tranCatCd()));
    }

    /** 1200-A-GET-DEFAULT-INT-RATE. */
    void getDefaultInterestRate(DisclosureGroupRecord.Key defaultKey) {
        Optional<DisclosureGroupRecord> found = disclosureGroupFile.read(defaultKey);
        if (found.isEmpty()) {
            abend("ERROR READING DEFAULT DISCLOSURE GROUP", "23");
        }
        disGroup = found.get();
    }

    /**
     * 1300-COMPUTE-INTEREST: monthly interest = balance x annual rate / 1200.
     * Like the COBOL COMPUTE (no ROUNDED), the result is cut to 2 decimals, not rounded.
     */
    void computeInterest() {
        monthlyInt = monthlyInterest(tranCatBal.balance(), disGroup.intRate());
        totalInt = Pic.fit(totalInt.add(monthlyInt), 9, 2);
        writeInterestTransaction();
    }

    static BigDecimal monthlyInterest(BigDecimal balance, BigDecimal annualRatePercent) {
        return Pic.fit(balance.multiply(annualRatePercent).divide(MONTHS_TIMES_PERCENT, 2, RoundingMode.DOWN), 9, 2);
    }

    /** 1300-B-WRITE-TX: write the interest transaction for the current category balance. */
    void writeInterestTransaction() {
        tranIdSuffix = (tranIdSuffix + 1) % 1_000_000;
        String timestamp = db2FormatTimestamp();
        transactionFile.write(new TransactionRecord(
                Pic.alpha(parmDate, 10) + Pic.unsigned(tranIdSuffix, 6),
                INTEREST_TRAN_TYPE_CD,
                INTEREST_TRAN_CAT_CD,
                INTEREST_TRAN_SOURCE,
                "Int. for a/c " + Pic.unsigned(account.acctId(), 11),
                monthlyInt,
                0L,
                "",
                "",
                "",
                xref.cardNum(),
                timestamp,
                timestamp));
    }

    /** 1400-COMPUTE-FEES: not implemented in CBACT04C either ("To be implemented"). */
    void computeFees() {
        // Intentionally empty, matching the COBOL program.
    }

    /** Z-GET-DB2-FORMAT-TIMESTAMP: current time as YYYY-MM-DD-HH.MM.SS.hh0000. */
    String db2FormatTimestamp() {
        LocalDateTime now = LocalDateTime.now(clock);
        int hundredths = now.getNano() / 10_000_000;
        return DB2_TS_PREFIX.format(now) + String.format("%02d", hundredths) + "0000";
    }

    /** 9910-DISPLAY-IO-STATUS followed by 9999-ABEND-PROGRAM. */
    private void abend(String message, String fileStatus) {
        sysout.println(message);
        sysout.println("FILE STATUS IS: NNNN00" + fileStatus);
        sysout.println("ABENDING PROGRAM");
        throw new AbendException(message, fileStatus);
    }
}
