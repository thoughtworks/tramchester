package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;

import java.io.PrintStream;

public interface Service extends HasId<Service>, GraphProperty {
    IdFor<Service> getId();

    void summariseDates(PrintStream printStream);

    ServiceCalendar getCalendar();
}
