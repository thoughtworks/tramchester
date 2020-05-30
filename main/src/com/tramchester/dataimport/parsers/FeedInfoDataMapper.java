package com.tramchester.dataimport.parsers;

import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;

public class FeedInfoDataMapper extends CSVEntryMapper<FeedInfo> {
    private static final Logger logger = LoggerFactory.getLogger(FeedInfoDataMapper.class);

    private final ProvidesNow providesNow;

    public FeedInfoDataMapper(ProvidesNow providesNow) {
        this.providesNow = providesNow;
    }

    @Override
    public FeedInfo parseEntry(CSVRecord data) {
        String publisherName = data.get(0);
        String publisherUrl = data.get(1);
        String timezone = data.get(2);
        String lang = data.get(3);
        LocalDate validFrom = parseDate(data.get(4), providesNow.getDate(), logger);
        LocalDate validTo = parseDate(data.get(5), providesNow.getDate(), logger);
        String version = data.get(6);

        return new FeedInfo(publisherName, publisherUrl, timezone, lang, validFrom, validTo, version);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        return true;
    }


}
