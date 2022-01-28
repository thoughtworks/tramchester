package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;

public class StationHelper {

    public static Station forTest(String id, String area, String stationName, LatLong latLong, DataSourceID dataSourceID) {
        IdFor<NaptanArea> areaId = StringIdFor.createId(area);
        return new MutableStation(Station.createId(id), areaId, stationName, latLong, CoordinateTransforms.getGridPosition(latLong), dataSourceID);
    }

    public static MutableStation forTestMutable(String id, String area, String stationName, LatLong latLong, DataSourceID dataSourceID) {
        IdFor<NaptanArea> areaId = StringIdFor.createId(area);
        return new MutableStation(Station.createId(id), areaId, stationName, latLong, CoordinateTransforms.getGridPosition(latLong), dataSourceID);
    }

}
