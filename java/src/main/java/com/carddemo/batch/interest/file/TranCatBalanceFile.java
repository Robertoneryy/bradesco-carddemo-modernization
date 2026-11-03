package com.carddemo.batch.interest.file;

import com.carddemo.batch.interest.copybook.TranCatBalanceRecord;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TCATBAL-FILE: indexed file read sequentially, so records come back in key order
 * (account id, then transaction type, then category). If the input holds the same key
 * twice, only the first occurrence is kept, as an indexed file cannot store duplicates.
 */
public final class TranCatBalanceFile {

    private final List<TranCatBalanceRecord> records;
    private int position;

    public TranCatBalanceFile(List<TranCatBalanceRecord> input) {
        Map<String, TranCatBalanceRecord> byKey = new LinkedHashMap<>();
        input.forEach(record -> byKey.putIfAbsent(record.key(), record));
        List<TranCatBalanceRecord> sorted = new ArrayList<>(byKey.values());
        sorted.sort(Comparator.comparing(TranCatBalanceRecord::key));
        this.records = List.copyOf(sorted);
    }

    public static TranCatBalanceFile load(Path path) {
        return new TranCatBalanceFile(RecordFiles.read(path, TranCatBalanceRecord::parse));
    }

    /** Returns the next record, or empty at end of file (file status '10'). */
    public Optional<TranCatBalanceRecord> readNext() {
        if (position >= records.size()) {
            return Optional.empty();
        }
        return Optional.of(records.get(position++));
    }
}
