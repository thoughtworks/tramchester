package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.CalendarData;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Service;
import com.tramchester.domain.ServiceCalendar;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class CalendarLoader {
    private static final Logger logger = LoggerFactory.getLogger(CalendarLoader.class);

    private final WriteableTransportData buildable;
    private final TransportEntityFactory factory;

    public CalendarLoader(WriteableTransportData buildable, TransportEntityFactory factory) {
        this.buildable = buildable;
        this.factory = factory;
    }

    public void load(Stream<CalendarData> calendars, IdMap<Service> services) {
        AtomicInteger countCalendars = new AtomicInteger(0);
        logger.info("Loading calendars for " + services.size() +" services ");

        IdSet<Service> missingCalendar = services.getIds();
        calendars.forEach(calendarData -> {
            IdFor<Service> serviceId = calendarData.getServiceId();
            MutableService service = buildable.getServiceById(serviceId);

            if (service != null) {
                countCalendars.getAndIncrement();
                missingCalendar.remove(serviceId);
                ServiceCalendar serviceCalendar = factory.createServiceCalendar(calendarData);
                service.setCalendar(serviceCalendar);
            } else {
                // legit, we filter services based on the route transport mode
                logger.debug("Unable to find service " + serviceId + " while populating Calendar");
            }
        });

        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar + " for calendar");
        }
        logger.info("Loaded " + countCalendars.get() + " calendar entries");

    }
}
