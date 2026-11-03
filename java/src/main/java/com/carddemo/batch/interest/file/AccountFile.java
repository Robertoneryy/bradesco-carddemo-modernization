package com.carddemo.batch.interest.file;

import com.carddemo.batch.interest.copybook.AccountRecord;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** ACCOUNT-FILE: indexed file opened for update, read and rewritten by ACCT-ID. */
public final class AccountFile {

    private final TreeMap<Long, AccountRecord> byAcctId = new TreeMap<>();

    public AccountFile(List<AccountRecord> input) {
        input.forEach(record -> byAcctId.putIfAbsent(record.acctId(), record));
    }

    public static AccountFile load(Path path) {
        return new AccountFile(RecordFiles.read(path, AccountRecord::parse));
    }

    public Optional<AccountRecord> read(long acctId) {
        return Optional.ofNullable(byAcctId.get(acctId));
    }

    /** Replaces an existing record. Returns false if no record with that ACCT-ID exists. */
    public boolean rewrite(AccountRecord record) {
        return byAcctId.replace(record.acctId(), record) != null;
    }

    /** All records in key order, as an unload of the indexed file would list them. */
    public List<AccountRecord> records() {
        return List.copyOf(byAcctId.values());
    }

    public void writeTo(Path path) {
        RecordFiles.write(path, records(), AccountRecord::toRecord);
    }
}
