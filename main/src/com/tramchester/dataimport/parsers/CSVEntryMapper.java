package com.tramchester.dataimport.parsers;

import org.apache.commons.csv.CSVRecord;

public interface CSVEntryMapper<T> {
    T parseEntry(CSVRecord data);
    boolean shouldInclude(CSVRecord data);
}
