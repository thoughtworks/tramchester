package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.DateRange;
import com.tramchester.graph.GraphPropertyKey;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public interface Route extends HasId<Route>, HasTransportMode, GraphProperty, CoreDomain {

    IdFor<Route> getId();

    String getName();

    Set<Service> getServices();

    Agency getAgency();

    String getShortName();

    TransportMode getTransportMode();

    @Override
    GraphPropertyKey getProp();

    Set<Trip> getTrips();

    boolean isDateOverlap(Route otherRoute);

    EnumSet<DayOfWeek> getOperatingDays();

    DateRange getDateRange();

    boolean isAvailableOn(LocalDate date);

    boolean intoNextDay();

    static IdFor<Route> createId(IdFor<Agency> agencyId, List<Station> callingPoints) {
        IdFor<Station> first = callingPoints.get(0).getId();
        IdFor<Station> last = callingPoints.get(callingPoints.size() - 1).getId();

        //CompositeId
        //         String firstName = callingPoints.get(0).getId().forDTO();
        //        String lastName = callingPoints.get(callingPoints.size()-1).getId().forDTO();
        //        return format("%s:%s=>%s", atocCode, firstName, lastName);
        return null;
    }

    static IdFor<Route> createId(String text) {
        return StringIdFor.createId(text);
    }

}
