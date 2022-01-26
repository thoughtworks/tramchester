package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.places.GroupedStations;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksNeighboursAndCompositeResourceTest {

    private static final AppConfiguration configuration = new NeighboursTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);
    private static GuiceContainerDependencies dependencies;

    private GroupedStations shudehillCompositeBus;
    private Station shudehillTram;
    private CompositeStationRepository compositeStationRepository;

    @BeforeAll
    public static void beforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    public void onceBeforeEachTest() {
        compositeStationRepository = dependencies.get(CompositeStationRepository.class);
        String shudehill_interchange = "Shudehill Interchange";
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
        assertEquals(2494, results.size(), "count of links");
    }

    @Test
    void shouldGetCompositeStations() {
        final String altrinchamInterchange = BusStations.Composites.AltrinchamInterchange.getName();
        GroupedStations actualComposite = compositeStationRepository.findByName(altrinchamInterchange);
        Set<String> expectedIds = actualComposite.getContained().stream().
                map(Station::forDTO).
                collect(Collectors.toSet());

        Response response = APIClient.getApiResponse(appExtension, "links/composites");
        assertEquals(200, response.getStatus(), "status");

        List<StationGroupDTO> groups = response.readEntity(new GenericType<>() {});
        assertFalse(groups.isEmpty());

        Optional<StationGroupDTO> found = groups.stream().
                filter(item -> item.getParent().getName().equals(altrinchamInterchange)).findFirst();
        assertTrue(found.isPresent());

        StationGroupDTO group = found.get();
        assertEquals(expectedIds.size(), group.getContained().size());

        Set<String> receivedIds = group.getContained().stream().map(StationRefDTO::getId).collect(Collectors.toSet());
        assertTrue(expectedIds.containsAll(receivedIds));

    }

    @NotNull
    private List<StationLinkDTO> getLinks() {

        Response response = APIClient.getApiResponse(appExtension, "links/neighbours");
        assertEquals(200, response.getStatus(), "status");

       return response.readEntity(new GenericType<>() {});

    }

}
