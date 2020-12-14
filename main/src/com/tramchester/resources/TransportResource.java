package com.tramchester.resources;

import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Optional;

public class TransportResource {
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
}
