package com.tramchester.dataimport.parsers;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Deprecated
public abstract class CSVEntryMapper<T> {
    private static final Logger logger = LoggerFactory.getLogger(CSVEntryMapper.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public abstract T parseEntry(CSVRecord data);

    public abstract boolean shouldInclude(CSVRecord data);

    protected final LocalDate parseDate(String str, LocalDate theDefault, Logger logger) {
        try {
            return LocalDate.parse(str, formatter);
        } catch (IllegalArgumentException unableToParse) {
            logger.warn(format("Unable to parse %s as a date", str), unableToParse);
            return theDefault;
        }
    }

    public final void initColumnIndex(CSVRecord csvRecord) {
        List<String> headers = new ArrayList<>(csvRecord.size());
        csvRecord.forEach(item -> headers.add(item.trim()));
        initColumnIndex(headers);
    }

    protected abstract void initColumnIndex(List<String> headers);

    protected final int findIndexOf(List<String> headers, ColumnDefination column) {
        int result = headers.indexOf(column.name());
        if (result==-1) {
            String msg = "Unable to find index for " + column.name() + " in " + headers;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return result;
    }

    public interface ColumnDefination  {
        String name();
    }
}
