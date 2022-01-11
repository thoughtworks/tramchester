package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.List;

import static java.lang.String.format;

public class MutableRailRoute extends MutableRoute {

    private final List<Station> callingPoints;

    public MutableRailRoute(IdFor<Route> id, List<Station> callingPoints, Agency agency, TransportMode transportMode) {
        super(id, createShortName(agency, callingPoints), createName(agency, callingPoints), agency, transportMode);
        if (callingPoints.size()<2) {
            final String message = format("Need at least 2 calling points route %s (%s) and calling points %s",
                    id, transportMode, callingPoints);
            throw new RuntimeException(message);
        }
        this.callingPoints = callingPoints;
    }

    public Station getBegin() {
        return callingPoints.get(0);
    }

    public Station getEnd() {
        return callingPoints.get(callingPoints.size()-1);
    }

    public List<Station> getCallingPoints() {
        return callingPoints;
    }

    @Override
    public String toString() {
        return "MutableRailRoute{" +
                "callingPoints=" + HasId.asIds(callingPoints) +
                "} " + super.toString();
    }

    private static String createShortName(Agency agency, List<Station> callingPoints) {
        Station first = callingPoints.get(0);
        int lastIndex = callingPoints.size() - 1;
        Station last = callingPoints.get(lastIndex);

        return format("%s service from %s to %s", agency.getName(), first.getName(), last.getName());

    }

    private static String createName(Agency agency, List<Station> callingPoints) {
        Station first = callingPoints.get(0);
        int lastIndex = callingPoints.size() - 1;
        Station last = callingPoints.get(lastIndex);
        StringBuilder result = new StringBuilder();

        result.append(format("%s service from %s to %s", agency.getName(), first.getName(), last.getName()));

        for (int i = 1; i < lastIndex; i++) {
            if (i>1) {
                result.append(", ");
            } else {
                result.append(" via ");
            }
            result.append(callingPoints.get(i).getName());
        }
        return result.toString();
    }

}
