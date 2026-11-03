//DUSRSECJ JOB 'DEF USRSEC FILE',REGION=8M,CLASS=A,
//      MSGCLASS=H,NOTIFY=&SYSUID
//******************************************************************
//* Copyright Amazon.com, Inc. or its affiliates.                   
//* All Rights Reserved.                                            
//*                                                                 
//* Licensed under the Apache License, Version 2.0 (the "License"). 
//* You may not use this file except in compliance with the License.
//* You may obtain a copy of the License at                         
//*                                                                 
//*    http://www.apache.org/licenses/LICENSE-2.0                   
//*                                                                 
//* Unless required by applicable law or agreed to in writing,      
//* software distributed under the License is distributed on an     
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,    
//* either express or implied. See the License for the specific     
//* language governing permissions and limitations under the License
//******************************************************************
//*-------------------------------------------------------------------*
//* PRE DELETE STEP
//*-------------------------------------------------------------------*
//*
//PREDEL  EXEC PGM=IEFBR14
//*
//DD01     DD DSN=AWS.M2.CARDDEMO.USRSEC.PS,
//            DISP=(MOD,DELETE,DELETE)
//*
//*-------------------------------------------------------------------*
//* CREATE USER SECURITY FILE (PS) FROM IN-STREAM USER LIST.
//* CBSEC01C GIVES EVERY USER A NEW RANDOM PASSWORD ON EACH RUN AND
//* PRINTS IT ONCE IN THIS JOB'S SYSOUT. NO PASSWORDS ARE STORED HERE.
//* RUN THIS JOB ONLY FOR THE FIRST LOAD; RERUNNING IT REPLACES ALL
//* USERS AND PASSWORDS.
//*-------------------------------------------------------------------*
//*
//STEP01  EXEC PGM=CBSEC01C
//STEPLIB  DD DISP=SHR,
//         DSN=AWS.M2.CARDDEMO.LOADLIB
//*
//USRSEED  DD *
ADMIN001MARGARET            GOLD                        A
ADMIN002RUSSELL             RUSSELL                     A
ADMIN003RAYMOND             WHITMORE                    A
ADMIN004EMMANUEL            CASGRAIN                    A
ADMIN005GRANVILLE           LACHAPELLE                  A
USER0001LAWRENCE            THOMAS                      U
USER0002AJITH               KUMAR                       U
USER0003LAURITZ             ALME                        U
USER0004AVERARDO            MAZZI                       U
USER0005LEE                 TING                        U
/*
//USRSEC   DD DSN=AWS.M2.CARDDEMO.USRSEC.PS,
//            DISP=(NEW,CATLG,DELETE),
//            DCB=(LRECL=80,RECFM=FB,DSORG=PS,BLKSIZE=0),
//            UNIT=SYSAD,SPACE=(TRK,(10,5),RLSE)
//*
//SYSOUT   DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//*
//*-------------------------------------------------------------------*
//* DEFINE VSAM FILE FOR USER SECURITY
//*-------------------------------------------------------------------*
//*
//STEP02  EXEC PGM=IDCAMS
//*
//SYSPRINT DD  SYSOUT=*
//SYSIN    DD  *
 DELETE                  AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS
 SET       MAXCC = 0
 DEFINE    CLUSTER (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS)    -
                    KEYS(8,0)                                 -
                    RECORDSIZE(80,80)                         -
                    REUSE                                     -
                    INDEXED                                   -
                    TRACKS(45,15)                             -
                    FREESPACE(10,15)                          -
                    CISZ(8192))                               -
           DATA    (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS.DAT)) -
           INDEX   (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS.IDX))
/*
//*
//*-------------------------------------------------------------------*
//* COPY USER SECURITY DATA FROM PS TO VSAM FILE
//*-------------------------------------------------------------------*
//*
//STEP03  EXEC PGM=IDCAMS
//*
//IN       DD  DSN=AWS.M2.CARDDEMO.USRSEC.PS,DISP=SHR
//OUT      DD  DSN=AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS,DISP=SHR
//SYSOUT   DD  SYSOUT=*
//SYSPRINT DD  SYSOUT=*
//SYSIN    DD  *
  REPRO INFILE(IN) OUTFILE(OUT)
/*
//*
//*-------------------------------------------------------------------*
//* DELETE THE WORK COPY SO THE NEW PASSWORDS ARE NOT LEFT IN A PS FILE
//*-------------------------------------------------------------------*
//*
//STEP04  EXEC PGM=IEFBR14,COND=(0,NE)
//*
//DD01     DD DSN=AWS.M2.CARDDEMO.USRSEC.PS,
//            DISP=(OLD,DELETE,KEEP)
//
//*
//* Ver: CardDemo_v1.0-15-g27d6c6f-68 Date: 2022-07-19 23:23:06 CDT
//*
