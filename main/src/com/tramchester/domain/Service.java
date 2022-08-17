package com.tramchester.domain;

import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.time.CrossesDay;
import com.tramchester.domain.time.TramTime;

import java.io.PrintStream;

public interface Service extends HasId<Service>, GraphProperty, CoreDomain, CrossesDay {
    IdFor<Service> getId();

    void summariseDates(PrintStream printStream);

    ServiceCalendar getCalendar();

    boolean hasCalendar();

    TramTime getStartTime();

    TramTime getFinishTime();

}
