package com.carddemo.batch.interest.copybook;

import java.math.BigDecimal;

/**
 * Account master record, copybook CVACT01Y (RECLN = 300).
 *
 * <pre>
 * 01  ACCOUNT-RECORD.
 *     05  ACCT-ID                  PIC 9(11).
 *     05  ACCT-ACTIVE-STATUS       PIC X(01).
 *     05  ACCT-CURR-BAL            PIC S9(10)V99.
 *     05  ACCT-CREDIT-LIMIT        PIC S9(10)V99.
 *     05  ACCT-CASH-CREDIT-LIMIT   PIC S9(10)V99.
 *     05  ACCT-OPEN-DATE           PIC X(10).
 *     05  ACCT-EXPIRAION-DATE      PIC X(10).
 *     05  ACCT-REISSUE-DATE        PIC X(10).
 *     05  ACCT-CURR-CYC-CREDIT     PIC S9(10)V99.
 *     05  ACCT-CURR-CYC-DEBIT      PIC S9(10)V99.
 *     05  ACCT-ADDR-ZIP            PIC X(10).
 *     05  ACCT-GROUP-ID            PIC X(10).
 *     05  FILLER                   PIC X(178).
 * </pre>
 */
public record AccountRecord(
        long acctId,
        String activeStatus,
        BigDecimal currBal,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        String openDate,
        String expirationDate,
        String reissueDate,
        BigDecimal currCycCredit,
        BigDecimal currCycDebit,
        String addrZip,
        String groupId,
        String filler) {

    public static final int LENGTH = 300;

    public static AccountRecord parse(String line) {
        return new AccountRecord(
                Pic.parseUnsigned(Pic.field(line, 0, 11)),
                Pic.field(line, 11, 1),
                Pic.parseZoned(Pic.field(line, 12, 12), 2),
                Pic.parseZoned(Pic.field(line, 24, 12), 2),
                Pic.parseZoned(Pic.field(line, 36, 12), 2),
                Pic.field(line, 48, 10),
                Pic.field(line, 58, 10),
                Pic.field(line, 68, 10),
                Pic.parseZoned(Pic.field(line, 78, 12), 2),
                Pic.parseZoned(Pic.field(line, 90, 12), 2),
                Pic.field(line, 102, 10),
                Pic.field(line, 112, 10),
                Pic.field(line, 122, 178));
    }

    public AccountRecord withBalances(BigDecimal newCurrBal, BigDecimal newCycCredit, BigDecimal newCycDebit) {
        return new AccountRecord(acctId, activeStatus, newCurrBal, creditLimit, cashCreditLimit,
                openDate, expirationDate, reissueDate, newCycCredit, newCycDebit, addrZip, groupId, filler);
    }

    public String toRecord() {
        return Pic.unsigned(acctId, 11)
                + Pic.alpha(activeStatus, 1)
                + Pic.zoned(currBal, 10, 2)
                + Pic.zoned(creditLimit, 10, 2)
                + Pic.zoned(cashCreditLimit, 10, 2)
                + Pic.alpha(openDate, 10)
                + Pic.alpha(expirationDate, 10)
                + Pic.alpha(reissueDate, 10)
                + Pic.zoned(currCycCredit, 10, 2)
                + Pic.zoned(currCycDebit, 10, 2)
                + Pic.alpha(addrZip, 10)
                + Pic.alpha(groupId, 10)
                + Pic.alpha(filler, 178);
    }
}
