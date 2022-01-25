package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.time.TramTime;

import java.io.PrintStream;

public interface Service extends HasId<Service>, GraphProperty, CoreDomain {
    IdFor<Service> getId();

    void summariseDates(PrintStream printStream);

    ServiceCalendar getCalendar();

    boolean hasCalendar();

    TramTime getStartTime();

    TramTime getFinishTime();
}
