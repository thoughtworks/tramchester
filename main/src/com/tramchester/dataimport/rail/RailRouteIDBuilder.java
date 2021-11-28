package com.tramchester.dataimport.rail;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RailRouteIDBuilder {
    private final Map<IdFor<Route>, Integer> baseIdToIndex;
    private final Map<List<IdFor<Station>>, IdFor<Route>> callingPointsToId;

    public RailRouteIDBuilder() {
        baseIdToIndex = new HashMap<>();
        callingPointsToId = new HashMap<>();
    }

    public IdFor<Route> getIdFor(String atocCode, List<Station> callingPoints) {
        List<IdFor<Station>> idList = callingPoints.stream().map(Station::getId).collect(Collectors.toList());
        if (callingPointsToId.containsKey(idList)) {
            return callingPointsToId.get(idList);
        }

        // else new combination of calling stations
        String baseIdText = createBaseIdForRoute(atocCode, callingPoints);
        IdFor<Route> baseId = StringIdFor.createId(baseIdText);

        int newIndex = 1;
        if (baseIdToIndex.containsKey(baseId)) {
            newIndex = baseIdToIndex.get(baseId) + 1;
        }

        baseIdToIndex.put(baseId, newIndex);
        IdFor<Route> finalId = StringIdFor.createId(format("%s:%s", baseIdText, newIndex));
        callingPointsToId.put(idList, finalId);
        return finalId;

    }

    private String createBaseIdForRoute(String atocCode, List<Station> callingPoints) {
        String firstName = callingPoints.get(0).getId().forDTO();
        String lastName = callingPoints.get(callingPoints.size()-1).getId().forDTO();
        String baseIdText = format("%s:%s=>%s", atocCode, firstName, lastName);
        return baseIdText;
    }
}
