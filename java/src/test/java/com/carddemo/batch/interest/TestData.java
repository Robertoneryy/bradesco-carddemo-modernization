package com.carddemo.batch.interest;

import java.nio.file.Path;

final class TestData {

    private TestData() {
    }

    /** app/data/ASCII, passed in by Maven (see pom.xml), with a fallback for IDE runs from java/. */
    static Path sampleDir() {
        return Path.of(System.getProperty("carddemo.data.dir", "../app/data/ASCII"));
    }
}
