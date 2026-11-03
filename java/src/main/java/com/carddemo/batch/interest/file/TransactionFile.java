package com.carddemo.batch.interest.file;

import com.carddemo.batch.interest.copybook.TransactionRecord;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** TRANSACT-FILE: sequential output file of 350-byte transaction records. */
public final class TransactionFile {

    private final List<TransactionRecord> records = new ArrayList<>();

    public void write(TransactionRecord record) {
        records.add(record);
    }

    public List<TransactionRecord> records() {
        return List.copyOf(records);
    }

    public void writeTo(Path path) {
        RecordFiles.write(path, records, TransactionRecord::toRecord);
    }
}
