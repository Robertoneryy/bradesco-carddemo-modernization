       IDENTIFICATION DIVISION.
       PROGRAM-ID. CBREFDRV.
      *    Test harness: loads the ASCII sample files into indexed
      *    files, runs CBACT04C, then unloads the account file.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT IN-TCATBAL ASSIGN TO INTCATB
                  ORGANIZATION IS LINE SEQUENTIAL.
           SELECT IN-XREF ASSIGN TO INXREF
                  ORGANIZATION IS LINE SEQUENTIAL.
           SELECT IN-ACCT ASSIGN TO INACCT
                  ORGANIZATION IS LINE SEQUENTIAL.
           SELECT IN-DISC ASSIGN TO INDISC
                  ORGANIZATION IS LINE SEQUENTIAL.
           SELECT OUT-ACCT ASSIGN TO OUTACCT
                  ORGANIZATION IS LINE SEQUENTIAL.
           SELECT TCATBAL-FILE ASSIGN TO TCATBALF
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS DYNAMIC
                  RECORD KEY   IS FD-TRAN-CAT-KEY
                  FILE STATUS  IS WS-STAT.
           SELECT XREF-FILE ASSIGN TO   XREFFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS DYNAMIC
                  RECORD KEY   IS FD-XREF-CARD-NUM
                  ALTERNATE RECORD KEY IS FD-XREF-ACCT-ID
                  FILE STATUS  IS WS-STAT.
           SELECT ACCOUNT-FILE ASSIGN TO ACCTFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS DYNAMIC
                  RECORD KEY   IS FD-ACCT-ID
                  FILE STATUS  IS WS-STAT.
           SELECT DISCGRP-FILE ASSIGN TO DISCGRP
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS DYNAMIC
                  RECORD KEY   IS FD-DISCGRP-KEY
                  FILE STATUS  IS WS-STAT.
       DATA DIVISION.
       FILE SECTION.
       FD  IN-TCATBAL.
       01  IN-TCATBAL-REC             PIC X(50).
       FD  IN-XREF.
       01  IN-XREF-REC                PIC X(50).
       FD  IN-ACCT.
       01  IN-ACCT-REC                PIC X(300).
       FD  IN-DISC.
       01  IN-DISC-REC                PIC X(50).
       FD  OUT-ACCT.
       01  OUT-ACCT-REC               PIC X(300).
       FD  TCATBAL-FILE.
       01  FD-TRAN-CAT-BAL-RECORD.
           05 FD-TRAN-CAT-KEY         PIC X(17).
           05 FILLER                  PIC X(33).
       FD  XREF-FILE.
       01  FD-XREFFILE-REC.
           05 FD-XREF-CARD-NUM        PIC X(16).
           05 FD-XREF-CUST-NUM        PIC 9(09).
           05 FD-XREF-ACCT-ID         PIC 9(11).
           05 FILLER                  PIC X(14).
       FD  DISCGRP-FILE.
       01  FD-DISCGRP-REC.
           05 FD-DISCGRP-KEY          PIC X(16).
           05 FILLER                  PIC X(34).
       FD  ACCOUNT-FILE.
       01  FD-ACCTFILE-REC.
           05 FD-ACCT-ID              PIC 9(11).
           05 FILLER                  PIC X(289).
       WORKING-STORAGE SECTION.
       01  WS-STAT                    PIC XX.
       01  WS-EOF                     PIC X.
       01  WS-PARM.
           05 WS-PARM-LEN             PIC S9(04) COMP VALUE 10.
           05 WS-PARM-DATE            PIC X(10).
       PROCEDURE DIVISION.
           ACCEPT WS-PARM-DATE FROM COMMAND-LINE
           OPEN INPUT IN-TCATBAL OUTPUT TCATBAL-FILE
           MOVE 'N' TO WS-EOF
           PERFORM UNTIL WS-EOF = 'Y'
              READ IN-TCATBAL AT END MOVE 'Y' TO WS-EOF
              NOT AT END
                 WRITE FD-TRAN-CAT-BAL-RECORD FROM IN-TCATBAL-REC
              END-READ
           END-PERFORM
           CLOSE IN-TCATBAL TCATBAL-FILE
           OPEN INPUT IN-XREF OUTPUT XREF-FILE
           MOVE 'N' TO WS-EOF
           PERFORM UNTIL WS-EOF = 'Y'
              READ IN-XREF AT END MOVE 'Y' TO WS-EOF
              NOT AT END
                 WRITE FD-XREFFILE-REC FROM IN-XREF-REC
              END-READ
           END-PERFORM
           CLOSE IN-XREF XREF-FILE
           OPEN INPUT IN-ACCT OUTPUT ACCOUNT-FILE
           MOVE 'N' TO WS-EOF
           PERFORM UNTIL WS-EOF = 'Y'
              READ IN-ACCT AT END MOVE 'Y' TO WS-EOF
              NOT AT END
                 WRITE FD-ACCTFILE-REC FROM IN-ACCT-REC
              END-READ
           END-PERFORM
           CLOSE IN-ACCT ACCOUNT-FILE
           OPEN INPUT IN-DISC OUTPUT DISCGRP-FILE
           MOVE 'N' TO WS-EOF
           PERFORM UNTIL WS-EOF = 'Y'
              READ IN-DISC AT END MOVE 'Y' TO WS-EOF
              NOT AT END
                 WRITE FD-DISCGRP-REC FROM IN-DISC-REC
              END-READ
           END-PERFORM
           CLOSE IN-DISC DISCGRP-FILE
           CALL 'CBACT04C' USING WS-PARM
           OPEN INPUT ACCOUNT-FILE OUTPUT OUT-ACCT
           MOVE 'N' TO WS-EOF
           PERFORM UNTIL WS-EOF = 'Y'
              READ ACCOUNT-FILE NEXT AT END MOVE 'Y' TO WS-EOF
              NOT AT END WRITE OUT-ACCT-REC FROM FD-ACCTFILE-REC
              END-READ
           END-PERFORM
           CLOSE ACCOUNT-FILE OUT-ACCT
           STOP RUN.
