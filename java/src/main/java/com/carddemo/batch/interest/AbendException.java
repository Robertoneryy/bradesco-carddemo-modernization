package com.carddemo.batch.interest;

/**
 * Raised where CBACT04C would stop abnormally (PERFORM 9999-ABEND-PROGRAM, abend code 999),
 * for example when an account, card cross-reference or default interest rate is missing.
 */
public class AbendException extends RuntimeException {

    public static final int ABEND_CODE = 999;

    private final String fileStatus;

    public AbendException(String message, String fileStatus) {
        super(message + " (file status " + fileStatus + ", abend code " + ABEND_CODE + ")");
        this.fileStatus = fileStatus;
    }

    public String fileStatus() {
        return fileStatus;
    }
}
