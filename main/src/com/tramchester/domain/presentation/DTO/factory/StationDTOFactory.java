package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@LazySingleton
public class StationDTOFactory {

    // NOTE: watch out for dependencies here, e.g. exchanges which would cause a DB build

    @Inject
    public StationDTOFactory() {
    }

    public StationRefDTO createStationRefDTO(Location<?> location) {
        return new StationRefDTO(location);
    }

    public StationRefWithPosition createStationRefWithPosition(Location<?> location) {
        return new StationRefWithPosition(location);
    }

    public StationGroupDTO createStationGroupDTO(StationGroup stationGroup) {
        String areaId = stationGroup.getAreaId().forDTO();
        List<StationRefWithPosition> contained = stationGroup.getContained().stream().
                map(this::createStationRefWithPosition).collect(Collectors.toList());
        return new StationGroupDTO(areaId, contained);
    }

    public StationLinkDTO createStationLinkDTO(StationLink stationLink) {
        StationRefWithPosition begin = createStationRefWithPosition(stationLink.getBegin());
        StationRefWithPosition end = createStationRefWithPosition(stationLink.getEnd());
        return new StationLinkDTO(begin, end, stationLink.getLinkingModes());
    }


    public StationNote createStationNote(Note.NoteType noteType, String text, Station station) {
        StationRefDTO stationRef = createStationRefDTO(station);
        return new StationNote(noteType, text, stationRef);
    }
}
