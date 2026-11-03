package com.carddemo.batch.interest.file;

import com.carddemo.batch.interest.copybook.DisclosureGroupRecord;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** DISCGRP-FILE: indexed file, read randomly by DIS-GROUP-KEY. */
public final class DisclosureGroupFile {

    private final Map<DisclosureGroupRecord.Key, DisclosureGroupRecord> byKey = new HashMap<>();

    public DisclosureGroupFile(List<DisclosureGroupRecord> input) {
        input.forEach(record -> byKey.putIfAbsent(record.key(), record));
    }

    public static DisclosureGroupFile load(Path path) {
        return new DisclosureGroupFile(RecordFiles.read(path, DisclosureGroupRecord::parse));
    }

    public Optional<DisclosureGroupRecord> read(DisclosureGroupRecord.Key key) {
        return Optional.ofNullable(byKey.get(key));
    }
}
