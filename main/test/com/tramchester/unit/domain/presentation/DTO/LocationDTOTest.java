package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class LocationDTOTest {

    @Test
    void shouldCreateDTOAsExpected() {

        MutableStation testStation = TestStation.forTest("9400ZZMAALT", "Altrincham area", "Altrincham",
                new LatLong(1,1), TransportMode.Tram, DataSourceID.tfgm);

        testStation.addRoute(TestEnv.getTramTestRoute(StringIdFor.createId("routeIdA"), "routeNameA"));
        testStation.addRoute(TestEnv.getTramTestRoute(StringIdFor.createId("routeIdB"), "routeNameB"));

        testStation.addPlatform(new Platform("9400ZZMAALT1", "Altrincham", new LatLong(1.2,1)));
        testStation.addPlatform(new Platform("9400ZZMAALT2", "Altrincham", new LatLong(1.1,1)));

        LocationDTO dto = new LocationDTO(testStation);

        assertEquals(testStation.forDTO(), dto.getId());
        assertEquals(testStation.getTransportModes(), dto.getTransportModes());
        assertEquals(testStation.getName(), dto.getName());
        assertEquals(testStation.getLatLong(), dto.getLatLong());
        assertTrue(dto.isTram());

//        assertEquals(2, dto.getRoutes().size());
//        Set<String> routeIds = dto.getRoutes().stream().map(RouteRefDTO::getId).collect(Collectors.toSet());
//        assertTrue(routeIds.contains("routeIdA"));
//        assertTrue(routeIds.contains("routeIdB"));

        assertEquals(2, dto.getRoutes().size());
        Set<String> routeNames = dto.getRoutes().stream().map(RouteRefDTO::getRouteName).collect(Collectors.toSet());
        assertTrue(routeNames.contains("routeNameA"));
        assertTrue(routeNames.contains("routeNameB"));

        assertTrue(dto.hasPlatforms());
        assertEquals(2, dto.getPlatforms().size());

        Optional<PlatformDTO> findPlatformOne = getPlatformById(dto, TramStations.Altrincham.forDTO() + "1");
        assertTrue(findPlatformOne.isPresent());
        PlatformDTO platformDTOA = findPlatformOne.get();
        assertEquals("Altrincham platform 1", platformDTOA.getName());
        assertEquals("1", platformDTOA.getPlatformNumber());

        Optional<PlatformDTO> findPlatformTwo = getPlatformById(dto, TramStations.Altrincham.forDTO() + "2");
        assertTrue(findPlatformTwo.isPresent());
        PlatformDTO platformDTOB = findPlatformTwo.get();
        assertEquals("Altrincham platform 2", platformDTOB.getName());
        assertEquals("2", platformDTOB.getPlatformNumber());

    }

    @NotNull
    private Optional<PlatformDTO> getPlatformById(LocationDTO dto, String platformId) {
        return dto.getPlatforms().stream().
                filter(platformDTO -> platformDTO.getId().equals(platformId)).findFirst();
    }

}
