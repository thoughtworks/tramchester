package com.tramchester.repository.naptan;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(NaptanRepositoryContainer.class)
public interface NaptanRepository {
    // TODO Check or diag on NaptanStopType
    <T extends Location<?>> boolean containsActo(IdFor<T> locationId);

    // TODO Check or diag on NaptanStopType
    <T extends Location<?>> NaptanRecord getForActo(IdFor<T> actoCode);

    NaptanRecord getForTiploc(IdFor<Station> railStationTiploc);

    boolean containsTiploc(IdFor<Station> tiploc);

    NaptanArea getAreaFor(IdFor<NaptanArea> id);

    boolean containsArea(IdFor<NaptanArea> id);

    IdSet<NaptanArea> activeCodes(IdSet<NaptanArea> ids);

    Set<NaptanRecord> getRecordsFor(IdFor<NaptanArea> areaId);

    boolean hasRecordsFor(IdFor<NaptanArea> areaId);

    Set<NaptanArea> getAreas();

    boolean isEnabled();
}
