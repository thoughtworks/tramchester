package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksResourceNeighboursTest {
    private static final AppConfiguration configuration = new NeighboursTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);
    private static GuiceContainerDependencies dependencies;
    private final String shudehill_interchange = "Shudehill Interchange";
    private CompositeStationRepository compositeStationRepository;
    private CompositeStation shudehillCompositeBus;
    private Station shudehillTram;

    @BeforeAll
    public static void beforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    public void onceBeforeEachTest() {
        compositeStationRepository = dependencies.get(CompositeStationRepository.class);
        shudehillCompositeBus = compositeStationRepository.findByName(shudehill_interchange);
        shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());
    }

    @Test
    void shouldGetStationNeighboursFromTram() {

        List<StationLinkDTO> results = getLinks();

        Set<String> fromShudehillTram = results.stream().
                filter(link -> link.getBegin().getId().equals(shudehillTram.forDTO())).
                map(link -> link.getEnd().getId()).
                collect(Collectors.toSet());

        assertFalse(fromShudehillTram.isEmpty());

        shudehillCompositeBus.getContained().forEach(busStop ->
                assertTrue(fromShudehillTram.contains(busStop.forDTO()), "missing " + busStop.forDTO()));
    }

    @Test
    void shouldGetStationNeighboursFromBus() {

        List<StationLinkDTO> results = getLinks();

        Set<String> busStopIds = shudehillCompositeBus.getContained().
                stream().
                map(Station::forDTO).
                collect(Collectors.toSet());

        final Set<StationLinkDTO> fromShudehillBusStops = results.stream().
                filter(link -> busStopIds.contains(link.getBegin().getId())).
                collect(Collectors.toSet());
        assertFalse(fromShudehillBusStops.isEmpty());

        Set<String> fromShudehillBusToTram = fromShudehillBusStops.stream().
                filter(link -> shudehillTram.forDTO().equals(link.getEnd().getId())).
                map(link -> link.getBegin().getId()).
                collect(Collectors.toSet());
        assertFalse(fromShudehillBusToTram.isEmpty());

    }

    @Test
    void expectedNumbers() {
        List<StationLinkDTO> results = getLinks();
        assertEquals(2488, results.size(), "count");
    }

    @NotNull
    private List<StationLinkDTO> getLinks() {
        String endPoint = "links/neighbours";

        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), "status");

       return response.readEntity(new GenericType<>() {});

    }

}
