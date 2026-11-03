package com.carddemo.batch.interest.file;

import com.carddemo.batch.interest.copybook.CardXrefRecord;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** XREF-FILE: indexed file, read randomly by its alternate key XREF-ACCT-ID. */
public final class CardXrefFile {

    private final Map<Long, CardXrefRecord> byAcctId = new HashMap<>();

    public CardXrefFile(List<CardXrefRecord> input) {
        input.forEach(record -> byAcctId.putIfAbsent(record.acctId(), record));
    }

    public static CardXrefFile load(Path path) {
        return new CardXrefFile(RecordFiles.read(path, CardXrefRecord::parse));
    }

    public Optional<CardXrefRecord> readByAcctId(long acctId) {
        return Optional.ofNullable(byAcctId.get(acctId));
    }
}
