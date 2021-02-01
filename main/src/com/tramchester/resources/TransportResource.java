package com.tramchester.resources;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Optional;

public abstract class TransportResource {
    private static final Logger logger = LoggerFactory.getLogger(TransportResource.class);
    protected final ProvidesNow providesNow;

    public TransportResource(ProvidesNow providesNow) {
        this.providesNow = providesNow;
    }

    protected TramTime parseOptionalTimeOrNow(String text) {
        Optional<TramTime> optionalTramTime = Optional.empty();
        if (!text.isEmpty()) {
            optionalTramTime = TramTime.parse(text);
            if (optionalTramTime.isEmpty()) {
                String msg = "Unable to parse time " + text;
                logger.warn(msg);
                throw new WebApplicationException(msg, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return optionalTramTime.orElseGet(providesNow::getNow);
    }

    protected TramTime parseTime(String text) {
        Optional<TramTime> maybeDepartureTime = TramTime.parse(text);
        if (maybeDepartureTime.isEmpty()) {
            String msg = "Could not parse time '" + text + "'";
            logger.error(msg);
            throw new WebApplicationException(msg, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return maybeDepartureTime.get();
    }

    protected boolean isHttps(String forwardedHeader) {
        return forwardedHeader != null && forwardedHeader.equalsIgnoreCase("https");
    }

    protected void guardForStationNotExisting(StationRepository repository, StringIdFor<Station> stationId) {
        if (!repository.hasStationId(stationId)) {
            String msg = "Unable to find station " + stationId;
            logger.warn(msg);
            throw new WebApplicationException(msg, Response.Status.NOT_FOUND);
        }
    }
}
