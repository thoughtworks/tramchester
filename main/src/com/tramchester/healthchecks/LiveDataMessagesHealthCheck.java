package com.tramchester.healthchecks;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.PlatformMessageRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static java.lang.String.format;

public class LiveDataMessagesHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataMessagesHealthCheck.class);

    private final PlatformMessageRepository repository;
    private final ProvidesNow currentTimeProvider;
    private final TramchesterConfig config;
    private final StationRepository stationRepository;

    public LiveDataMessagesHealthCheck(TramchesterConfig config, PlatformMessageRepository repository,
                                       ProvidesNow currentTimeProvider, StationRepository stationRepository) {
        this.config = config;
        this.repository = repository;
        this.currentTimeProvider = currentTimeProvider;
        this.stationRepository = stationRepository;
    }

    // normally only between 2 and 4 missing
    // private static final int MISSING_MSGS_LIMIT = 4;
    // during night hours gradually goes to zero than back to full about 6.05am

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int entries = repository.numberOfEntries();

        if (entries==0) {
            String msg = "No entires present";
            logger.warn(msg);
            return Result.unhealthy(msg);
        }

        int numberStations = stationRepository.getNumberOfStations();
        LocalDateTime dateTime = currentTimeProvider.getDateTime();
        int stationsWithMessages = repository.numberStationsWithMessages(dateTime);

        int offset = numberStations - stationsWithMessages;
        boolean lateNight = isLateNight(dateTime);

        if (offset > config.getMaxNumberStationsWithoutMessages()) {
            if (!lateNight) {
                String message = format("Not enough messages present, %s entries, %s out of %s stations",
                        entries, stationsWithMessages, numberStations);
                logger.warn(message);
                return Result.unhealthy(message);
            }
        }

        String msg = format("Live data messages healthy with %s entries for %s out of %s stations ",
                entries, stationsWithMessages, numberStations);
        logger.info(msg);
        return Result.healthy(msg);
    }

    @Override
    public String getName() {
        return "liveDataMessages";
    }
}
