package com.tramchester.domain.id;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

public class RailRouteId implements IdFor<Route> {
    final IdFor<Station> begin;
    final IdFor<Station> end;
    final IdFor<Agency> agencyId;
    final int index;
    final String asString;

    public RailRouteId(IdFor<Station> begin, IdFor<Station> end, IdFor<Agency> agencyId, int index) {
        this.begin = begin;
        this.end = end;
        this.agencyId = agencyId;
        this.index = index;
        asString = createStringId();
    }

    private String createStringId() {
        String firstName = begin.forDTO();
        String lastName = end.forDTO();
        return format("%s:%s=>%s:%s", agencyId.forDTO(), firstName, lastName, index);
    }

    public static RailRouteId createId(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints, int index) {
        if (callingPoints.size()<2) {
            throw new RuntimeException("Need at least two calling points to create rail route id, got " + callingPoints);
        }
        IdFor<Station> first = callingPoints.get(0);
        IdFor<Station> last = callingPoints.get(callingPoints.size()-1);
        return new RailRouteId(first, last, agencyId, index);
    }

    @Override
    public String forDTO() {
        return asString;
    }

    @Override
    public String getGraphId() {
        return asString;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Class<Route> getDomainType() {
        return Route.class;
    }

    @Override
    public String toString() {
        return "RailRouteId{" +
                "asString=" + asString +
                ", begin=" + begin +
                ", end=" + end +
                ", agencyId=" + agencyId +
                ", index=" + index +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RailRouteId that = (RailRouteId) o;
        return index == that.index && begin.equals(that.begin) && end.equals(that.end) && agencyId.equals(that.agencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end, agencyId, index);
    }
}
