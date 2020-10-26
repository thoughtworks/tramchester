package com.tramchester.healthchecks;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.LiveDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class LiveDataMessagesHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataMessagesHealthCheck.class);

    private final LiveDataRepository repository;
    private final ProvidesNow currentTimeProvider;
    private final TramchesterConfig config;

    private final TramTime startOfNight = TramTime.of(2,0);
    private final TramTime endOfNight = TramTime.of(6, 10);

    public LiveDataMessagesHealthCheck(TramchesterConfig config, LiveDataRepository repository, ProvidesNow currentTimeProvider) {
        this.config = config;
        this.repository = repository;
        this.currentTimeProvider = currentTimeProvider;
    }

    // normally only between 2 and 4 missing
    //private static final int MISSING_MSGS_LIMIT = 4;

    // during night hours gradually goes to zero than back to full about 6.05am

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int entries = repository.upToDateEntries();
        int messages = repository.countEntriesWithMessages();

        int offset = entries - messages;
        boolean lateNight = currentTimeProvider.getNow().between(startOfNight, endOfNight);

        if (offset>config.getMaxNumberMissingLiveMessages()) {
            if (!lateNight) {
                String message = format("Not enough messages present, %s out of %s entries", messages, entries);
                logger.warn(message);
                return Result.unhealthy(message);
            }
        }

        String msg = format("Live data messages healthy with %s entries", messages);
        logger.info(msg);
        return Result.healthy(msg);
    }

    @Override
    public String getName() {
        return "liveDataMessages";
    }
}
