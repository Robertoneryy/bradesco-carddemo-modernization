package com.carddemo.batch.interest.copybook;

import java.math.BigDecimal;

/**
 * Disclosure group (interest rate) record, copybook CVTRA02Y (RECLN = 50).
 *
 * <pre>
 * 01  DIS-GROUP-RECORD.
 *     05  DIS-GROUP-KEY.
 *        10 DIS-ACCT-GROUP-ID     PIC X(10).
 *        10 DIS-TRAN-TYPE-CD      PIC X(02).
 *        10 DIS-TRAN-CAT-CD       PIC 9(04).
 *     05  DIS-INT-RATE            PIC S9(04)V99.
 *     05  FILLER                  PIC X(28).
 * </pre>
 *
 * {@code DIS-INT-RATE} is an annual percentage, e.g. 15.00 means 15% a year.
 */
public record DisclosureGroupRecord(
        Key key,
        BigDecimal intRate,
        String filler) {

    public static final int LENGTH = 50;

    /** DIS-GROUP-KEY. Text parts are stored padded to their PIC X width, as in the file. */
    public record Key(String acctGroupId, String tranTypeCd, int tranCatCd) {
        public Key {
            acctGroupId = Pic.alpha(acctGroupId, 10);
            tranTypeCd = Pic.alpha(tranTypeCd, 2);
        }
    }

    public static DisclosureGroupRecord parse(String line) {
        return new DisclosureGroupRecord(
                new Key(
                        Pic.field(line, 0, 10),
                        Pic.field(line, 10, 2),
                        (int) Pic.parseUnsigned(Pic.field(line, 12, 4))),
                Pic.parseZoned(Pic.field(line, 16, 6), 2),
                Pic.field(line, 22, 28));
    }

    public String toRecord() {
        return key.acctGroupId() + key.tranTypeCd() + Pic.unsigned(key.tranCatCd(), 4)
                + Pic.zoned(intRate, 4, 2) + Pic.alpha(filler, 28);
    }
}
