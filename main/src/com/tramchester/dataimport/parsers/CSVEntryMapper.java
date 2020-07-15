package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.datacleanse.TransportDataWriter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;

public abstract class CSVEntryMapper<T> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public abstract T parseEntry(CSVRecord data);

    public abstract boolean shouldInclude(CSVRecord data);

    protected LocalDate parseDate(String str, LocalDate theDefault, Logger logger) {
        try {
            return LocalDate.parse(str, formatter);
        } catch (IllegalArgumentException unableToParse) {
            logger.warn(format("Unable to parse %s as a date", str), unableToParse);
            return theDefault;
        }
    }

    // TODO Make abstract once all mappers finished
    public void writeHeader(TransportDataWriter writer) {
        // noop
    }

    // TODO Make abstract once all mappers finished
    public void initColumnIndex(CSVRecord header) {
        // noop
    }
}
