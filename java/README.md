# Interest calculation batch (Java version of CBACT04C)

This folder holds the Java 17 version of the COBOL batch program
[`app/cbl/CBACT04C.cbl`](../app/cbl/CBACT04C.cbl), the monthly interest calculator run by
the `INTCALC` job.

## What the program does

For every transaction category balance of every account it:

1. finds the account and the card linked to it;
2. looks up the yearly interest rate for the account's disclosure group, transaction type
   and category, and uses the `DEFAULT` group when the account's group has no rate;
3. when the rate is not zero, computes one month of interest
   (`balance x rate / 1200`, cut, not rounded, to cents) and writes an interest transaction;
4. adds the account's total interest to its current balance and resets the cycle credit
   and debit totals to zero.

The Java code keeps the COBOL behaviour on purpose, including two quirks:

* the last account in the balance file gets its interest transactions but its balance is
  not updated (the COBOL loop ends before posting it);
* amounts too large for their COBOL field lose their leftmost digits instead of failing.

## Build and test

```bash
cd java
mvn verify
```

## Run

```bash
java -jar target/carddemo-batch-interest-1.0.0-SNAPSHOT.jar 2022071800 ../app/data/ASCII out
```

The first argument is the run date passed as `PARM` in `INTCALC.jcl`. The input folder must
contain `tcatbal.txt`, `cardxref.txt`, `acctdata.txt` and `discgrp.txt`. The job writes
`out/systran.txt` (new transactions) and `out/acctdata.txt` (updated accounts).

## How the tests prove nothing changed

* `CopybookRecordsTest` reads every record of the sample files in `app/data/ASCII` into the
  Java records and writes it back, byte for byte.
* `InterestCalculationJobTest` checks each business rule and the run on the sample data.
* `CobolEquivalenceTest` runs the scenarios in `src/test/resources/scenarios` and compares the
  transactions, updated accounts and program messages with the output of the original
  COBOL program. Those expected files are produced by
  [`cobol-reference/generate-expected.sh`](cobol-reference/generate-expected.sh), which
  compiles and runs `CBACT04C.cbl` with GnuCOBOL. CI re-runs that script and fails if the
  expected files no longer match the COBOL output.
