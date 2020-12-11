package com.tramchester.healthchecks;

import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.DueTramsRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;

import static java.lang.String.format;

@Singleton
public class LiveDataHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHealthCheck.class);

    private final DueTramsRepository repository;
    private final ProvidesNow providesNow;
    private final StationRepository stationRepository;
    private final LiveDataConfig config;

    @Inject
    public LiveDataHealthCheck(DueTramsRepository repository, ProvidesNow providesNow, StationRepository stationRepository, TramchesterConfig config) {
        this.repository = repository;
        this.providesNow = providesNow;
        this.stationRepository = stationRepository;
        this.config = config.getLiveDataConfig();
    }

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int entries = repository.upToDateEntries();

        String noEntriesPresent = "no entries present";

        if (entries==0) {
            logger.error(noEntriesPresent);
            return Result.unhealthy(noEntriesPresent);
        }

        int numberOfStations = stationRepository.getNumberOfStations();
        LocalDateTime dateTime = providesNow.getDateTime();
        int stationsWithData = repository.getNumStationsWithData(dateTime);
        int offset = numberOfStations-stationsWithData;

        if (offset > config.getMaxNumberStationsWithoutData()) {
            if (!isLateNight(dateTime)) {
                String msg = format("Only %s of %s stations have data, %s entries present", stationsWithData,
                        numberOfStations, entries);

                logger.warn(msg);
                return Result.unhealthy(msg);
            }
        }

        String msg = format("Live data healthy with %s entires and %s of %s stations", entries, stationsWithData,
                numberOfStations);
        logger.info(msg);
        return Result.healthy(msg);
    }

    @Override
    public String getName() {
        return "liveData";
    }
}
