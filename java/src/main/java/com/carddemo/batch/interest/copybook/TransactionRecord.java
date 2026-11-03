package com.carddemo.batch.interest.copybook;

import java.math.BigDecimal;

/**
 * Transaction record, copybook CVTRA05Y (RECLN = 350).
 *
 * <pre>
 * 01  TRAN-RECORD.
 *     05  TRAN-ID                 PIC X(16).
 *     05  TRAN-TYPE-CD            PIC X(02).
 *     05  TRAN-CAT-CD             PIC 9(04).
 *     05  TRAN-SOURCE             PIC X(10).
 *     05  TRAN-DESC               PIC X(100).
 *     05  TRAN-AMT                PIC S9(09)V99.
 *     05  TRAN-MERCHANT-ID        PIC 9(09).
 *     05  TRAN-MERCHANT-NAME      PIC X(50).
 *     05  TRAN-MERCHANT-CITY      PIC X(50).
 *     05  TRAN-MERCHANT-ZIP       PIC X(10).
 *     05  TRAN-CARD-NUM           PIC X(16).
 *     05  TRAN-ORIG-TS            PIC X(26).
 *     05  TRAN-PROC-TS            PIC X(26).
 *     05  FILLER                  PIC X(20).
 * </pre>
 */
public record TransactionRecord(
        String tranId,
        String typeCd,
        int catCd,
        String source,
        String desc,
        BigDecimal amt,
        long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String cardNum,
        String origTs,
        String procTs) {

    public static final int LENGTH = 350;

    public static TransactionRecord parse(String line) {
        return new TransactionRecord(
                Pic.field(line, 0, 16),
                Pic.field(line, 16, 2),
                (int) Pic.parseUnsigned(Pic.field(line, 18, 4)),
                Pic.field(line, 22, 10),
                Pic.field(line, 32, 100),
                Pic.parseZoned(Pic.field(line, 132, 11), 2),
                Pic.parseUnsigned(Pic.field(line, 143, 9)),
                Pic.field(line, 152, 50),
                Pic.field(line, 202, 50),
                Pic.field(line, 252, 10),
                Pic.field(line, 262, 16),
                Pic.field(line, 278, 26),
                Pic.field(line, 304, 26));
    }

    public String toRecord() {
        return Pic.alpha(tranId, 16)
                + Pic.alpha(typeCd, 2)
                + Pic.unsigned(catCd, 4)
                + Pic.alpha(source, 10)
                + Pic.alpha(desc, 100)
                + Pic.zoned(amt, 9, 2)
                + Pic.unsigned(merchantId, 9)
                + Pic.alpha(merchantName, 50)
                + Pic.alpha(merchantCity, 50)
                + Pic.alpha(merchantZip, 10)
                + Pic.alpha(cardNum, 16)
                + Pic.alpha(origTs, 26)
                + Pic.alpha(procTs, 26)
                + Pic.alpha("", 20);
    }
}
