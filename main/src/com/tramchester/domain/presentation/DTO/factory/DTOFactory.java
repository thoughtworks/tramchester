package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@LazySingleton
public class DTOFactory {

    // NOTE: watch out for dependencies here, e.g. Exchanges which would cause a full DB build

    @Inject
    public DTOFactory() {
    }

    public LocationRefDTO createLocationRefDTO(Location<?> location) {
        return new LocationRefDTO(location);
    }

    public LocationRefWithPosition createLocationRefWithPosition(Location<?> location) {
        return new LocationRefWithPosition(location);
    }

    public StationGroupDTO createStationGroupDTO(StationGroup stationGroup) {
        String areaId = stationGroup.getAreaId().forDTO();
        List<LocationRefWithPosition> contained = stationGroup.getContained().stream().
                map(this::createLocationRefWithPosition).collect(Collectors.toList());
        return new StationGroupDTO(areaId, contained);
    }

    public StationLinkDTO createStationLinkDTO(StationLink stationLink) {
        LocationRefWithPosition begin = createLocationRefWithPosition(stationLink.getBegin());
        LocationRefWithPosition end = createLocationRefWithPosition(stationLink.getEnd());
        return new StationLinkDTO(begin, end, stationLink.getLinkingModes());
    }


    public StationNote createStationNote(Note.NoteType noteType, String text, Station station) {
        LocationRefDTO stationRef = createLocationRefDTO(station);
        return new StationNote(noteType, text, stationRef);
    }
}
