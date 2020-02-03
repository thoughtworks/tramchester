package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.LiveDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StationDTOFactory {
    private final LiveDataRepository liveDataRepository;

    public StationDTOFactory(LiveDataRepository liveDataRepository) {
        this.liveDataRepository = liveDataRepository;
    }

    public StationDTO build(Station station, TramServiceDate queryDate, TramTime queryTime) {
        List<PlatformDTO> platformDTOS = new ArrayList<>();

//        TramServiceDate serviceDate = new TramServiceDate(localDateTime.toLocalDate());
//        TramTime time = TramTime.of(localDateTime.toLocalTime());

        station.getPlatforms().forEach(platform -> {
            PlatformDTO platformDTO = new PlatformDTO(platform);
            Optional<StationDepartureInfo> departInfo = liveDataRepository.departuresFor(platform, queryDate, queryTime);
            departInfo.ifPresent(info -> platformDTO.setDepartureInfo(new StationDepartureInfoDTO(info)));
            platformDTOS.add(platformDTO);
        });
        return new StationDTO(station, platformDTOS, ProximityGroup.NEAREST_STOPS);
    }
}
