package com.carddemo.batch.interest.copybook;

/**
 * Card cross-reference record, copybook CVACT03Y (RECLN = 50).
 *
 * <pre>
 * 01 CARD-XREF-RECORD.
 *     05  XREF-CARD-NUM          PIC X(16).
 *     05  XREF-CUST-ID           PIC 9(09).
 *     05  XREF-ACCT-ID           PIC 9(11).
 *     05  FILLER                 PIC X(14).
 * </pre>
 */
public record CardXrefRecord(
        String cardNum,
        long custId,
        long acctId,
        String filler) {

    public static final int LENGTH = 50;

    public static CardXrefRecord parse(String line) {
        return new CardXrefRecord(
                Pic.field(line, 0, 16),
                Pic.parseUnsigned(Pic.field(line, 16, 9)),
                Pic.parseUnsigned(Pic.field(line, 25, 11)),
                Pic.field(line, 36, 14));
    }

    public String toRecord() {
        return Pic.alpha(cardNum, 16) + Pic.unsigned(custId, 9) + Pic.unsigned(acctId, 11)
                + Pic.alpha(filler, 14);
    }
}
