package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.presentation.DTO.StationClosureDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.StationClosureForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
public class StationResourceClosedStationTest {
    private final static TramServiceDate when = new TramServiceDate(TestEnv.testDay());

    private static final TramStations closedStation = TramStations.StPetersSquare;

    private final static List<StationClosure> closedStations = Collections.singletonList(
            new StationClosureForTest(closedStation, when.getDate(), when.getDate().plusWeeks(1)));

    // NOTE: planning disabled here
    private static final AppConfiguration config = new IntegrationTramClosedStationsTestConfig(
            "closed_stpeters_int_test_tram.db", closedStations, false);

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, config);

    @Test
    void shouldGetClosedStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/closures");
        assertEquals(200, result.getStatus());

        List<StationClosureDTO> results = result.readEntity(new GenericType<>() {});

        assertEquals(1, results.size());
        StationClosureDTO stationClosure = results.get(0);
        List<LocationRefDTO> stations = stationClosure.getStations();
        assertEquals(1, stations.size());
        assertEquals(closedStation.getRawId(), stations.get(0).getId());
        assertEquals(when.getDate(), stationClosure.getBegin());
        assertEquals(when.getDate().plusWeeks(1), stationClosure.getEnd());
    }

}
