package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;

import java.util.List;


/***
 * Note: serializable for RouteIndex cache purposes
 */
public class RailRouteId extends ContainsId<Route> implements IdFor<Route> {

    private final IdFor<Station> begin;
    private final IdFor<Station> end;
    private final IdFor<Agency> agencyId;
    private final int index;

    @JsonCreator
    public RailRouteId(@JsonProperty("begin") IdFor<Station> begin,
                       @JsonProperty("end") IdFor<Station> end,
                       @JsonProperty("agencyId") IdFor<Agency> agencyId,
                       @JsonProperty("index") int index) {

        super(createContainedId(begin, end, agencyId, index));
        this.begin = begin;
        this.end = end;
        this.agencyId = agencyId;
        this.index = index;

    }

    public static RailRouteId createId(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints, int index) {
        if (callingPoints.size()<2) {
            throw new RuntimeException("Need at least two calling points to create rail route id, got " + callingPoints);
        }
        IdFor<Station> first = callingPoints.get(0);
        IdFor<Station> last = callingPoints.get(callingPoints.size()-1);
        return new RailRouteId(first, last, agencyId, index);
    }

    private static StringIdFor<Route> createContainedId(IdFor<Station> begin, IdFor<Station> end, IdFor<Agency> agency, int index) {
        // TODO Turn IdFor into abstract class so can have package private access to contained string ids?
        StringIdFor<Station> beginId = (StringIdFor<Station>) begin;
        StringIdFor<Station> endId = (StringIdFor<Station>) end;
        StringIdFor<Agency> agencyId = (StringIdFor<Agency>) agency;

        String idText = String.format("%s:%s=>%s:%s", beginId.getContainedId(), endId.getContainedId(),
                agencyId.getContainedId(), index);

        return new StringIdFor<>(idText, Route.class);
    }

    @JsonIgnore
    @Override
    public String getGraphId() {
        return super.getGraphId();
    }

    @JsonIgnore
    @Override
    public boolean isValid() {
        return true;
    }

    @JsonIgnore
    @Override
    public Class<Route> getDomainType() {
        return Route.class;
    }

    public IdFor<Station> getBegin() {
        return begin;
    }

    public IdFor<Station> getEnd() {
        return end;
    }

    public IdFor<Agency> getAgencyId() {
        return agencyId;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "RailRouteId{" +
                "begin=" + begin +
                ", end=" + end +
                ", agencyId=" + agencyId +
                ", index=" + index +
                "} " + super.toString();
    }

}
