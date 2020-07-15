package com.tramchester.dataimport.parsers;

import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class FeedInfoDataMapper extends CSVEntryMapper<FeedInfo> {
    private static final Logger logger = LoggerFactory.getLogger(FeedInfoDataMapper.class);
    private int indexOfPublisherName = -1;
    private int indexOfPublisherURL = -1;
    private int indexOfTimezone = -1;
    private int indexOfLang = -1;
    private int indexOfValidFrom = -1;
    private int indexOfValidTo = -1;
    private int indexOfVersion = -1;

    private enum Columns implements ColumnDefination {
        feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,feed_valid_from,feed_valid_to,feed_version
    }

    private final ProvidesNow providesNow;

    public FeedInfoDataMapper(ProvidesNow providesNow) {
        this.providesNow = providesNow;
    }

    @Override
    public FeedInfo parseEntry(CSVRecord data) {
        String publisherName = data.get(indexOfPublisherName);
        String publisherUrl = data.get(indexOfPublisherURL);
        String timezone = data.get(indexOfTimezone);
        String lang = data.get(indexOfLang);
        LocalDate validFrom = parseDate(data.get(indexOfValidFrom), providesNow.getDate(), logger);
        LocalDate validTo = parseDate(data.get(indexOfValidTo), providesNow.getDate(), logger);
        String version = data.get(indexOfVersion);

        return new FeedInfo(publisherName, publisherUrl, timezone, lang, validFrom, validTo, version);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        return true;
    }

    @Override
    protected ColumnDefination[] getColumns() {
        return Columns.values();
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfPublisherName = findIndexOf(headers, Columns.feed_publisher_name);
        indexOfPublisherURL = findIndexOf(headers, Columns.feed_publisher_url);
        indexOfTimezone = findIndexOf(headers, Columns.feed_timezone);
        indexOfLang = findIndexOf(headers, Columns.feed_lang);
        indexOfValidFrom = findIndexOf(headers, Columns.feed_valid_from);
        indexOfValidTo = findIndexOf(headers, Columns.feed_valid_to);
        indexOfVersion = findIndexOf(headers, Columns.feed_version);
    }

}
