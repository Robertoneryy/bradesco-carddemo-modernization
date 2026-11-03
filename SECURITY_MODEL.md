# CardDemo security model (threat model)

Scope: `Robertoneryy/bradesco-carddemo-modernization` (primary) and `Robertoneryy/bradesco-carddemo-backup`. Both are at the same commit (`59cc6c2`).

## User-provided facts
- **Deployment:** demo/reference only, with synthetic data. No real cardholder or customer data is processed. Findings matter as modernization quality: the Java port has to fix or knowingly carry each weakness.
- **Attackers:** all of them matter. That means signed-on regular users, senders of MQ messages, and insiders with dataset, JCL or batch access.
- **Infrastructure:** there is no infrastructure repository. RACF, CICS region, MQ and AWS M2 config are not in scope and can't be verified. Assume defaults: no transaction-level RACF security.
- **Mirror repo:** `bradesco-carddemo-backup` is a mirror of the modernization repo. A backup finding with the same root cause and location as a modernization finding is a duplicate.
- **Accepted risk:** nothing beyond the mirror duplicates.

## System overview and trust boundaries
CardDemo is a credit-card management app written in COBOL and run under CICS on z/OS or AWS M2. Its data lives in VSAM KSDS files, and batch work runs as JCL scheduled by Control-M/CA7. Optional extensions add IMS DB, Db2 and IBM MQ. Trust boundaries:
1. 3270 terminal user → CICS online transactions (BMS maps). After sign-on, state travels in the COMMAREA (`app/cpy/COCOM01Y.cpy`) between pseudo-conversational programs.
2. MQ request queues → CICS MQ-triggered programs. These take the reply queue from the untrusted MQMD.
3. Dataset/JCL users → batch programs that read and write the master files.
4. Developer workstation → mainframe, via FTP-tunnel scripts.

## Components
| Component | Code |
|---|---|
| **Online-Auth** | `app/cbl/COSGN00C.cbl`, `app/bms/COSGN00.bms`, `app/cpy/CSUSR01Y.cpy` (USRSEC record) |
| **Online-Admin** | `app/cbl/COADM01C.cbl`, `app/cbl/COUSR0[0-3]C.cbl`, `app/cpy/COADM02Y.cpy`, CICS resource definitions in `app/csd/` |
| **Online-Customer** | `app/cbl/COMEN01C.cbl`, `COACT*`, `COCRD*`, `COTRN0*`, `COBIL00C`, `CORPT00C`, `app/cpy/COMEN02Y.cpy`, `app/cpy/COCOM01Y.cpy` |
| **Batch** | `app/cbl/CB*.cbl`, `app/jcl/`, `app/proc/`, `app/scheduler/`, `app/ctl/`, `app/asm/`, `samples/` |
| **Ext-Authorization (IMS/Db2/MQ)** | `app/app-authorization-ims-db2-mq/` |
| **Ext-TranType (Db2)** | `app/app-transaction-type-db2/` |
| **Ext-VSAM-MQ** | `app/app-vsam-mq/` (COACCT01 / CDRA, CODATE01 / CDRD) |
| **Data-Fixtures** | `app/data/`, `app/catlg/`, sample data embedded in JCL |
| **Tooling** | `scripts/` (FTP to Ensono via localhost:2121 tunnel) |

## Entry points
- CICS transactions in `app/csd/CARDDEMO.CSD`: CC00 sign-on, CM00 menu, CA00 admin, CU00–CU03 user admin, CAUP/CAVW accounts, CCLI/CCDL/CCUP cards, CT00–CT02 transactions, CB00 bill pay, CR00 reports. Extensions add CP00/CPVS/CPVD, CTLI and CDRA/CDRD.
- MQ queues read by COPAUA0C, COACCT01 and CODATE01.
- Batch JCL (38 jobs) and the Control-M/CA7 schedules.

## Assets
- USRSEC user file: IDs, 8-char plaintext passwords, user type (A/U).
- Account, card, customer (including SSN), cross-reference and transaction master files, and Db2/IMS authorization records. All of this is synthetic here.
- Integrity of balances and transaction postings (batch CBTRN02C, CBACT04C).

## Existing controls
- Sign-on compares the uppercased password to USRSEC (`COSGN00C.cbl` ~L209–245). There is no hashing, lockout or audit.
- Admin options are gated by `CDEMO-USRTYP-ADMIN` in the COMMAREA (`COMEN01C.cbl` L137). Programs started with `EIBCALEN = 0` mostly bounce to sign-on. Exception: `COTRTLIC.cbl` L515–520 sets admin on a cold start.
- No RACF transaction or resource security is defined in the repo.

## Observations (my reading of the code, not user input)
- Plaintext passwords, and demo credentials embedded in JCL (`app/jcl/ESDSRRDS.jcl`).
- COMMAREA-based authorization. Admin programs (COUSR0xC) don't re-check the user type.
- COPAUA0C replies to an MQMD reply-to queue that the sender controls.

## Open questions
- How will authorization be enforced in the target (Java/AWS) runtime? RACF-equivalent, or app-level?
