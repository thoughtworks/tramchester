package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.Set;
import java.util.stream.Collectors;


class LocationDTOTest {

    @Test
    void shouldCreateDTOAsExpected() throws TransformException {

        Station testStation = TestStation.forTest("9400ZZMAALT", "Altrincham area", "Altrincham", new LatLong(1,1), TransportMode.Tram);

        testStation.addRoute(TestEnv.getTestRoute(IdFor.createId("routeIdA")));
        testStation.addRoute(TestEnv.getTestRoute(IdFor.createId("routeIdB")));

        testStation.addPlatform(new Platform("9400ZZMAALT1", "Altrincham"));
        testStation.addPlatform(new Platform("9400ZZMAALT2", "Altrincham"));

        LocationDTO dto = new LocationDTO(testStation);

        Assertions.assertEquals(testStation.forDTO(), dto.getId());
        Assertions.assertEquals(testStation.getTransportMode(), dto.getTransportMode());
        Assertions.assertEquals(testStation.getName(), dto.getName());
        Assertions.assertEquals(testStation.getLatLong(), dto.getLatLong());
        Assertions.assertTrue(dto.isTram());

        Assertions.assertEquals(2, dto.getRoutes().size());
        Set<String> routeIds = dto.getRoutes().stream().map(RouteRefDTO::getId).collect(Collectors.toSet());
        Assertions.assertTrue(routeIds.contains("routeIdA"));
        Assertions.assertTrue(routeIds.contains("routeIdB"));

        Assertions.assertTrue(dto.hasPlatforms());
        Assertions.assertEquals(2, dto.getPlatforms().size());

        PlatformDTO platformDTOA = dto.getPlatforms().get(0);
        Assertions.assertEquals(Stations.Altrincham.forDTO()+"1", platformDTOA.getId());
        Assertions.assertEquals("Altrincham platform 1", platformDTOA.getName());
        Assertions.assertEquals("1", platformDTOA.getPlatformNumber());


        PlatformDTO platformDTOB = dto.getPlatforms().get(1);
        Assertions.assertEquals(Stations.Altrincham.forDTO()+"2", platformDTOB.getId());
        Assertions.assertEquals("Altrincham platform 2", platformDTOB.getName());
        Assertions.assertEquals("2", platformDTOB.getPlatformNumber());


    }

}
