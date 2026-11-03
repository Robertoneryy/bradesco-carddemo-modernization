package com.carddemo.batch.interest;

import com.carddemo.batch.interest.file.AccountFile;
import com.carddemo.batch.interest.file.CardXrefFile;
import com.carddemo.batch.interest.file.DisclosureGroupFile;
import com.carddemo.batch.interest.file.TranCatBalanceFile;
import com.carddemo.batch.interest.file.TransactionFile;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Command-line entry point, the equivalent of the INTCALC job step.
 *
 * <pre>
 * java -jar carddemo-batch-interest.jar &lt;parm-date&gt; &lt;input-dir&gt; &lt;output-dir&gt;
 * </pre>
 *
 * The input directory holds tcatbal.txt, cardxref.txt, acctdata.txt and discgrp.txt in the
 * layout of app/data/ASCII. The job writes systran.txt (new interest transactions) and
 * acctdata.txt (updated accounts) to the output directory. Nothing is written if it abends.
 */
public final class InterestCalculationMain {

    private InterestCalculationMain() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: <parm-date e.g. 2022071800> <input-dir> <output-dir>");
            System.exit(2);
        }
        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);

        AccountFile accounts = AccountFile.load(input.resolve("acctdata.txt"));
        TransactionFile transactions = new TransactionFile();
        InterestCalculationJob job = new InterestCalculationJob(
                args[0],
                TranCatBalanceFile.load(input.resolve("tcatbal.txt")),
                CardXrefFile.load(input.resolve("cardxref.txt")),
                DisclosureGroupFile.load(input.resolve("discgrp.txt")),
                accounts,
                transactions,
                Clock.systemDefaultZone(),
                System.out);
        try {
            job.run();
        } catch (AbendException e) {
            System.err.println("ABEND U" + AbendException.ABEND_CODE + ": " + e.getMessage());
            System.exit(1);
        }
        transactions.writeTo(output.resolve("systran.txt"));
        accounts.writeTo(output.resolve("acctdata.txt"));
    }
}
