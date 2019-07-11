package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.FeedInfo;
import com.tramchester.repository.LatestFeedInfoRepository;
import com.tramchester.repository.ProvidesFeedInfo;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class NewDataAvailableHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(NewDataAvailableHealthCheck.class);

    private final LatestFeedInfoRepository repository;
    private final ProvidesFeedInfo current;

    public NewDataAvailableHealthCheck(LatestFeedInfoRepository repository, ProvidesFeedInfo current){

        this.repository = repository;
        this.current = current;
    }

    @Override
    protected Result check() {
        FeedInfo currentFeedInfo = current.getFeedInfo();
        FeedInfo newFeedInfo = repository.getFeedinfo();

        logger.info("Checking if newer timetable data available");

        LocalDate currentEnd = currentFeedInfo.validUntil();
        LocalDate newStart = newFeedInfo.validFrom();

        if (currentEnd.equals(newFeedInfo.validUntil()) && newStart.equals(currentFeedInfo.validFrom())) {
            logger.info("No new feedinfo available");
            return Result.healthy();
        }

        return Result.unhealthy("Newer timetable is available " + newFeedInfo.toString());

    }
}
