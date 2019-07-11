package com.tramchester.repository;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.parsers.FeedInfoDataParser;
import com.tramchester.domain.FeedInfo;
import com.tramchester.healthchecks.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.Dependencies.TFGM_UNZIP_DIR;
import static java.lang.String.format;

public class LatestFeedRepository {
    private static final Logger logger = LoggerFactory.getLogger(LatestFeedRepository.class);

    private final FetchDataFromUrl fetchDataFromUrl;
    private final ProvidesNow providesNow;
    private FeedInfo currentInfo;
    private LocalDate lastUpdate;

    public LatestFeedRepository(FetchDataFromUrl fetchDataFromUrl, ProvidesNow providesNow) {
        this.fetchDataFromUrl = fetchDataFromUrl;
        this.providesNow = providesNow;
    }

    // once a day
    public FeedInfo getFeedinfo() {
        LocalDate queryTime = providesNow.getDate();
        if (lastUpdate==null) {
            lastUpdate = queryTime.minusDays(1);
        }
        if (queryTime.isAfter(lastUpdate)) {
            List<FeedInfo> answer = fetchLatest();
            if (answer.size()==1) {
                currentInfo = answer.get(0);
            } else {
                logger.warn(format("Unable to update curent feedinfo, found %s entries", answer.size()));
            }
        }
        lastUpdate = queryTime;
        return currentInfo;
    }


    private List<FeedInfo> fetchLatest() {

        List<FeedInfo> result = new LinkedList<>();

        try {
            ByteArrayInputStream inputStream = fetchDataFromUrl.streamForFile(format("%s/%s.txt", TFGM_UNZIP_DIR,
                    TransportDataReader.FEED_INFO));

            InputStreamReader reader = new InputStreamReader(inputStream);

            CSVStrategy csvStrategy = new CSVStrategy(',', '"', '#', true, true);
            CSVReader<FeedInfo> cvsParser = new CSVReaderBuilder<FeedInfo>(reader).
                    entryParser(new FeedInfoDataParser()).strategy(csvStrategy).
                    build();
            result = cvsParser.readAll();
            reader.close();
        } catch (IOException exception) {
            logger.warn(format("Unable to fetch latest data exception was %s", exception ));
        }

        return result;
    }

}
