package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.domain.FeedInfo;

public class FeedInfoDataParser implements CSVEntryParser<FeedInfo> {
    @Override
    public FeedInfo parseEntry(String... data) {
        String publisherName = data[0];
        String publisherUrl = data[1];
        String timezone = data[2];
        String lang = data[3];
        String validFrom = data[4];
        String validTo = data[5];
        String version = data[6];

        return new FeedInfo(publisherName, publisherUrl, timezone, lang, validFrom,validTo,version);
    }
}
