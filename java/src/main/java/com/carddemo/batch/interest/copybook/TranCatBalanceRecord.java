package com.carddemo.batch.interest.copybook;

import java.math.BigDecimal;

/**
 * Transaction category balance record, copybook CVTRA01Y (RECLN = 50).
 *
 * <pre>
 * 01  TRAN-CAT-BAL-RECORD.
 *     05  TRAN-CAT-KEY.
 *        10 TRANCAT-ACCT-ID       PIC 9(11).
 *        10 TRANCAT-TYPE-CD       PIC X(02).
 *        10 TRANCAT-CD            PIC 9(04).
 *     05  TRAN-CAT-BAL            PIC S9(09)V99.
 *     05  FILLER                  PIC X(22).
 * </pre>
 */
public record TranCatBalanceRecord(
        long acctId,
        String typeCd,
        int catCd,
        BigDecimal balance,
        String filler) {

    public static final int LENGTH = 50;

    public static TranCatBalanceRecord parse(String line) {
        return new TranCatBalanceRecord(
                Pic.parseUnsigned(Pic.field(line, 0, 11)),
                Pic.field(line, 11, 2),
                (int) Pic.parseUnsigned(Pic.field(line, 13, 4)),
                Pic.parseZoned(Pic.field(line, 17, 11), 2),
                Pic.field(line, 28, 22));
    }

    /** The 17-byte record key (TRAN-CAT-KEY) used to order the indexed file. */
    public String key() {
        return Pic.unsigned(acctId, 11) + Pic.alpha(typeCd, 2) + Pic.unsigned(catCd, 4);
    }

    public String toRecord() {
        return key() + Pic.zoned(balance, 9, 2) + Pic.alpha(filler, 22);
    }
}
