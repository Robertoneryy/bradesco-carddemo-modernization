      ******************************************************************
      * PROGRAM     : CBSEC01C.CBL
      * Application : CardDemo
      * Type        : BATCH COBOL Program
      * FUNCTION    : BUILD THE USER SECURITY FILE FROM A LIST OF USERS,
      *               GIVING EACH USER A NEW GENERATED PASSWORD.
      *               THE PASSWORDS ARE PRINTED ONCE TO SYSOUT AND ARE
      *               NEVER TAKEN FROM THE INPUT.
      ******************************************************************
      * Copyright Amazon.com, Inc. or its affiliates.
      * All Rights Reserved.
      *
      * Licensed under the Apache License, Version 2.0 (the "License").
      * You may not use this file except in compliance with the License.
      * You may obtain a copy of the License at
      *
      *    http://www.apache.org/licenses/LICENSE-2.0
      *
      * Unless required by applicable law or agreed to in writing,
      * software distributed under the License is distributed on an
      * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
      * either express or implied. See the License for the specific
      * language governing permissions and limitations under the License
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID.    CBSEC01C.
       AUTHOR.        AWS.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT USRSEED-FILE ASSIGN TO USRSEED
                  ORGANIZATION IS SEQUENTIAL
                  ACCESS MODE  IS SEQUENTIAL
                  FILE STATUS  IS USRSEED-STATUS.
      *
           SELECT USRSEC-FILE ASSIGN TO USRSEC
                  ORGANIZATION IS SEQUENTIAL
                  ACCESS MODE  IS SEQUENTIAL
                  FILE STATUS  IS USRSEC-STATUS.
      *
       DATA DIVISION.
       FILE SECTION.
       FD  USRSEED-FILE.
       01  FD-USRSEED-REC                    PIC X(80).

       FD  USRSEC-FILE.
       01  FD-USRSEC-REC                     PIC X(80).

       WORKING-STORAGE SECTION.

      *****************************************************************
       COPY CSUSR01Y.

       01  USRSEED-STATUS.
           05  USRSEED-STAT1       PIC X.
           05  USRSEED-STAT2       PIC X.
       01  USRSEC-STATUS.
           05  USRSEC-STAT1        PIC X.
           05  USRSEC-STAT2        PIC X.

       01  IO-STATUS.
           05  IO-STAT1            PIC X.
           05  IO-STAT2            PIC X.
       01  TWO-BYTES-BINARY        PIC 9(4) BINARY.
       01  TWO-BYTES-ALPHA         REDEFINES TWO-BYTES-BINARY.
           05  TWO-BYTES-LEFT      PIC X.
           05  TWO-BYTES-RIGHT     PIC X.
       01  IO-STATUS-04.
           05  IO-STATUS-0401      PIC 9   VALUE 0.
           05  IO-STATUS-0403      PIC 999 VALUE 0.

       01  END-OF-FILE             PIC X(01)    VALUE 'N'.
       01  ABCODE                  PIC S9(9) BINARY.
       01  TIMING                  PIC S9(9) BINARY.

      * Password characters. Look-alikes (0/O, 1/I) are left out.
      * Sign-on upper-cases what the user types, so no lower case.
       01  WS-PWD-CHARSET          PIC X(32)
               VALUE 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'.
       01  WS-PWD-CHARSET-LEN      PIC 9(02)    VALUE 32.

       01  WS-CURRENT-DATE         PIC X(21).
       01  WS-CURRENT-DATE-N       REDEFINES WS-CURRENT-DATE.
           05  WS-CD-YYYYMMDD      PIC 9(08).
           05  WS-CD-HHMMSSHH      PIC 9(08).
           05  FILLER              PIC X(05).
       01  WS-SEED                 PIC 9(10)    VALUE 0.
       01  WS-RANDOM               PIC V9(09)   VALUE 0.
       01  WS-CHAR-IX              PIC 9(02)    VALUE 0.
       01  WS-PWD-IX               PIC 9(02)    VALUE 0.
       01  WS-NEW-PWD              PIC X(08)    VALUE SPACES.
       01  WS-USER-COUNT           PIC 9(05)    VALUE 0.

      *****************************************************************
       PROCEDURE DIVISION.
           DISPLAY 'START OF EXECUTION OF PROGRAM CBSEC01C'.
           PERFORM 0000-USRSEED-OPEN.
           PERFORM 0100-USRSEC-OPEN.
           PERFORM 0200-SEED-RANDOM.

           DISPLAY '*** INITIAL PASSWORDS - GIVE EACH ONE ONLY TO ITS '
                   'OWNER AND HAVE IT CHANGED AFTER FIRST SIGN-ON ***'
           DISPLAY 'USER ID   TYPE  INITIAL PASSWORD'

           PERFORM UNTIL END-OF-FILE = 'Y'
               PERFORM 1000-USRSEED-GET-NEXT
               IF  END-OF-FILE = 'N'
               AND SEC-USR-ID NOT = SPACES
                   PERFORM 2000-GENERATE-PASSWORD
                   MOVE WS-NEW-PWD TO SEC-USR-PWD
                   MOVE SPACES     TO SEC-USR-FILLER
                   PERFORM 3000-USRSEC-WRITE
                   DISPLAY SEC-USR-ID '  ' SEC-USR-TYPE '     '
                           SEC-USR-PWD
                   ADD 1 TO WS-USER-COUNT
               END-IF
           END-PERFORM.

           DISPLAY 'USERS LOADED: ' WS-USER-COUNT
           MOVE SPACES TO WS-NEW-PWD SEC-USER-DATA

           PERFORM 9000-USRSEED-CLOSE.
           PERFORM 9100-USRSEC-CLOSE.

           DISPLAY 'END OF EXECUTION OF PROGRAM CBSEC01C'.

           GOBACK.

      *****************************************************************
      * Seed the generator from the current date and time so that  *
      * every run produces different passwords.                    *
      *****************************************************************
       0200-SEED-RANDOM.
           MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE
           COMPUTE WS-SEED = FUNCTION MOD(
                       WS-CD-HHMMSSHH * 100000
                     + FUNCTION MOD(WS-CD-YYYYMMDD, 100000)
                     , 2147483646) + 1
           COMPUTE WS-RANDOM = FUNCTION RANDOM(WS-SEED)
           EXIT.
      *---------------------------------------------------------------*
       1000-USRSEED-GET-NEXT.
           READ USRSEED-FILE INTO SEC-USER-DATA
           EVALUATE USRSEED-STATUS
               WHEN '00'
                   CONTINUE
               WHEN '10'
                   MOVE 'Y' TO END-OF-FILE
               WHEN OTHER
                   DISPLAY 'ERROR READING USER SEED FILE'
                   MOVE USRSEED-STATUS TO IO-STATUS
                   PERFORM 9910-DISPLAY-IO-STATUS
                   PERFORM 9999-ABEND-PROGRAM
           END-EVALUATE
           EXIT.
      *---------------------------------------------------------------*
       2000-GENERATE-PASSWORD.
           MOVE SPACES TO WS-NEW-PWD
           PERFORM VARYING WS-PWD-IX FROM 1 BY 1
                   UNTIL WS-PWD-IX > LENGTH OF WS-NEW-PWD
               COMPUTE WS-RANDOM = FUNCTION RANDOM
               COMPUTE WS-CHAR-IX =
                   FUNCTION INTEGER(WS-RANDOM * WS-PWD-CHARSET-LEN) + 1
               MOVE WS-PWD-CHARSET(WS-CHAR-IX:1)
                 TO WS-NEW-PWD(WS-PWD-IX:1)
           END-PERFORM
           EXIT.
      *---------------------------------------------------------------*
       3000-USRSEC-WRITE.
           WRITE FD-USRSEC-REC FROM SEC-USER-DATA
           IF  USRSEC-STATUS NOT = '00'
               DISPLAY 'ERROR WRITING USER SECURITY FILE'
               MOVE USRSEC-STATUS TO IO-STATUS
               PERFORM 9910-DISPLAY-IO-STATUS
               PERFORM 9999-ABEND-PROGRAM
           END-IF
           EXIT.
      *---------------------------------------------------------------*
       0000-USRSEED-OPEN.
           OPEN INPUT USRSEED-FILE
           IF  USRSEED-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING USER SEED FILE'
               MOVE USRSEED-STATUS TO IO-STATUS
               PERFORM 9910-DISPLAY-IO-STATUS
               PERFORM 9999-ABEND-PROGRAM
           END-IF
           EXIT.
      *---------------------------------------------------------------*
       0100-USRSEC-OPEN.
           OPEN OUTPUT USRSEC-FILE
           IF  USRSEC-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING USER SECURITY FILE'
               MOVE USRSEC-STATUS TO IO-STATUS
               PERFORM 9910-DISPLAY-IO-STATUS
               PERFORM 9999-ABEND-PROGRAM
           END-IF
           EXIT.
      *---------------------------------------------------------------*
       9000-USRSEED-CLOSE.
           CLOSE USRSEED-FILE
           IF  USRSEED-STATUS NOT = '00'
               DISPLAY 'ERROR CLOSING USER SEED FILE'
               MOVE USRSEED-STATUS TO IO-STATUS
               PERFORM 9910-DISPLAY-IO-STATUS
               PERFORM 9999-ABEND-PROGRAM
           END-IF
           EXIT.
      *---------------------------------------------------------------*
       9100-USRSEC-CLOSE.
           CLOSE USRSEC-FILE
           IF  USRSEC-STATUS NOT = '00'
               DISPLAY 'ERROR CLOSING USER SECURITY FILE'
               MOVE USRSEC-STATUS TO IO-STATUS
               PERFORM 9910-DISPLAY-IO-STATUS
               PERFORM 9999-ABEND-PROGRAM
           END-IF
           EXIT.

       9999-ABEND-PROGRAM.
           DISPLAY 'ABENDING PROGRAM'
           MOVE 0 TO TIMING
           MOVE 999 TO ABCODE
           CALL 'CEE3ABD' USING ABCODE, TIMING.

      *****************************************************************
       9910-DISPLAY-IO-STATUS.
           IF  IO-STATUS NOT NUMERIC
           OR  IO-STAT1 = '9'
               MOVE IO-STAT1 TO IO-STATUS-04(1:1)
               MOVE 0        TO TWO-BYTES-BINARY
               MOVE IO-STAT2 TO TWO-BYTES-RIGHT
               MOVE TWO-BYTES-BINARY TO IO-STATUS-0403
               DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04
           ELSE
               MOVE '0000' TO IO-STATUS-04
               MOVE IO-STATUS TO IO-STATUS-04(3:2)
               DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04
           END-IF
           EXIT.
