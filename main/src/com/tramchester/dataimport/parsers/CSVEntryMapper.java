package com.tramchester.dataimport.parsers;

import org.apache.commons.csv.CSVRecord;

public interface CSVEntryMapper<T> {
    T parseEntry(CSVRecord data);
    boolean filter(CSVRecord data);
}
