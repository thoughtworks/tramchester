package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.domain.FeedInfo;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class FeedInfoDataParser implements CSVEntryParser<FeedInfo> {
    private static final Logger logger = LoggerFactory.getLogger(FeedInfoDataParser.class);

    private static org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYYMMdd");

    @Override
    public FeedInfo parseEntry(String... data) {
        String publisherName = data[0];
        String publisherUrl = data[1];
        String timezone = data[2];
        String lang = data[3];
        LocalDate validFrom = parseDate(data[4]);
        LocalDate validTo = parseDate(data[5]);
        String version = data[6];

        return new FeedInfo(publisherName, publisherUrl, timezone, lang, validFrom, validTo, version);
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
