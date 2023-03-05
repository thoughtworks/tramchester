package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksNeighboursAndCompositeResourceTest {

    private static final AppConfiguration configuration = new NeighboursTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);
    private static GuiceContainerDependencies dependencies;

    private StationGroup shudehillCompositeBus;
    private Station shudehillTram;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    public static void beforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    public void onceBeforeEachTest() {
        stationGroupsRepository = dependencies.get(StationGroupsRepository.class);
        StationRepository stationRepository = dependencies.get(StationRepository.class);

        String shudehill_interchange = "Shudehill Interchange";
        shudehillCompositeBus = stationGroupsRepository.findByName(shudehill_interchange);
        shudehillTram = stationRepository.getStationById(Shudehill.getId());
    }

    @Test
    void shouldGetStationNeighboursFromTram() {

        List<StationLinkDTO> results = getLinks();

        Set<IdForDTO> fromShudehillTram = results.stream().
                filter(link -> link.getBegin().getId().equals(IdForDTO.createFor(shudehillTram))).
                map(link -> link.getEnd().getId()).
                collect(Collectors.toSet());

        assertFalse(fromShudehillTram.isEmpty());

        shudehillCompositeBus.getContained().forEach(busStop ->
                assertTrue(fromShudehillTram.contains(IdForDTO.createFor(busStop)), "missing " + busStop.forDTO()));
    }

    @Test
    void shouldGetStationNeighboursFromBus() {

        List<StationLinkDTO> results = getLinks();

        Set<IdForDTO> busStopIds = shudehillCompositeBus.getContained().
                stream().
                map(IdForDTO::createFor).
                collect(Collectors.toSet());

        final Set<StationLinkDTO> fromShudehillBusStops = results.stream().
                filter(link -> busStopIds.contains(link.getBegin().getId())).
                collect(Collectors.toSet());

        assertFalse(fromShudehillBusStops.isEmpty());

        Set<IdForDTO> fromShudehillBusToTram = fromShudehillBusStops.stream().
                filter(link -> IdForDTO.createFor(shudehillTram).equals(link.getEnd().getId())).
                map(link -> link.getBegin().getId()).
                collect(Collectors.toSet());

        assertFalse(fromShudehillBusToTram.isEmpty());
    }

    @Test
    void expectedNumbers() {
        List<StationLinkDTO> results = getLinks();
        assertEquals(2476, results.size(), "count of links");
    }

    @Test
    void shouldGetCompositeStations() {
        final String altrinchamInterchangeName = BusStations.Composites.AltrinchamInterchange.getName();
        StationGroup actualComposite = stationGroupsRepository.findByName(altrinchamInterchangeName);
        Set<String> expectedIds = actualComposite.getContained().stream().
                map(Station::forDTO).
                collect(Collectors.toSet());

        Response response = APIClient.getApiResponse(appExtension, "links/composites");
        assertEquals(200, response.getStatus(), "status");

        List<StationGroupDTO> groups = response.readEntity(new GenericType<>() {});
        assertFalse(groups.isEmpty());

        Optional<StationGroupDTO> found = groups.stream().
                filter(item -> item.getAreaId().equals(actualComposite.getAreaId().forDTO())).findFirst();
        assertTrue(found.isPresent());

        StationGroupDTO group = found.get();
        assertEquals(expectedIds.size(), group.getContained().size());

        Set<String> receivedIds = group.getContained().stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                collect(Collectors.toSet());

        assertTrue(expectedIds.containsAll(receivedIds));
    }

    @NotNull
    private List<StationLinkDTO> getLinks() {

        Response response = APIClient.getApiResponse(appExtension, "links/neighbours");
        assertEquals(200, response.getStatus(), "status");

       return response.readEntity(new GenericType<>() {});

    }

}
