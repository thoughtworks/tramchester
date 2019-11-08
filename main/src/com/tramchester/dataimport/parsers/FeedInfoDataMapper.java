package com.tramchester.dataimport.parsers;

import com.tramchester.domain.FeedInfo;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;

public class FeedInfoDataMapper implements CSVEntryMapper<FeedInfo> {
    private static final Logger logger = LoggerFactory.getLogger(FeedInfoDataMapper.class);
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public FeedInfo parseEntry(CSVRecord data) {
        String publisherName = data.get(0);
        String publisherUrl = data.get(1);
        String timezone = data.get(2);
        String lang = data.get(3);
        LocalDate validFrom = parseDate(data.get(4));
        LocalDate validTo = parseDate(data.get(5));
        String version = data.get(6);

        return new FeedInfo(publisherName, publisherUrl, timezone, lang, validFrom, validTo, version);
    }

    @Override
    public boolean filter(CSVRecord data) {
        return true;
    }

    private LocalDate parseDate(String str) {
        try {
            return LocalDate.parse(str, formatter);
        } catch (IllegalArgumentException unableToParse) {
            logger.warn(format("Unable to parse %s as a date", str), unableToParse);
            return LocalDate.now();
        }
    }
}
