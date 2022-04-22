package com.tramchester.unit.repository;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.*;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.Bury;

public class LiveDataUpdaterTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataUpdater repository;
    private ProvidesNow providesNow;
    private LocalDateTime lastUpdate;
    private TramDepartureRepository tramDepartureRepository;
    private PlatformMessageRepository platformMessageRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        providesNow = createMock(ProvidesNow.class);
        platformMessageRepository = createMock(PlatformMessageRepository.class);
        tramDepartureRepository = createMock(TramDepartureRepository.class);

        repository = new LiveDataUpdater(platformMessageRepository, tramDepartureRepository, fetcher, mapper, providesNow);

        lastUpdate = TestEnv.LocalNow();
    }

    @Test
    void shouldUpdateRepositories() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        info.add(createDepartureInfoWithDueTram(lastUpdate, "yyy", "platformIdA",
                "some message", Altrincham.fake()));
        info.add(createDepartureInfoWithDueTram(lastUpdate, "303", "platformIdB",
                "some message", Altrincham.fake()));

        EasyMock.expect(providesNow.getNowHourMins()).andStubReturn(TramTime.ofHourMins(lastUpdate.toLocalTime()));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        EasyMock.expect(platformMessageRepository.updateCache(info)).andReturn(2);
        EasyMock.expect(tramDepartureRepository.updateCache(info)).andReturn(2);

        replayAll();
        repository.refreshRespository();
        verifyAll();
    }

    @Test
    void shouldUpdateRepositoriesIgnoringStaleData() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        Station station = Altrincham.fake();
        TramStationDepartureInfo departureInfo = createDepartureInfoWithDueTram(lastUpdate, "yyy", "platformIdA",
                "some message", station);

        info.add(departureInfo);
        info.add(createDepartureInfoWithDueTram(lastUpdate.plusMinutes(25), "303", "platformIdB",
                "some message", station));
        info.add(createDepartureInfoWithDueTram(lastUpdate.minusMinutes(25), "444", "platformIdC",
                "some message", station));

        EasyMock.expect(providesNow.getNowHourMins()).andStubReturn(TramTime.ofHourMins(lastUpdate.toLocalTime()));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        List<TramStationDepartureInfo> expected = Collections.singletonList(departureInfo);
        EasyMock.expect(platformMessageRepository.updateCache(expected)).andReturn(1);
        EasyMock.expect(tramDepartureRepository.updateCache(expected)).andReturn(1);

        replayAll();
        repository.refreshRespository();
        verifyAll();
    }

    public static TramStationDepartureInfo createDepartureInfoWithDueTram(LocalDateTime lastUpdate,
                                                                          String displayId, String platformId, String message,
                                                                          Station station) {
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId, Lines.Airport,
                LineDirection.Incoming, station, message, lastUpdate);
        departureInfo.setStationPlatform(TestEnv.createPlatformFor(station, platformId));
        UpcomingDeparture dueTram = new UpcomingDeparture(lastUpdate.toLocalDate(), station, Bury.fake(),
                "Due", TramTime.ofHourMins(lastUpdate.toLocalTime()).plusMinutes(42), "Single",
                TestEnv.MetAgency(), TransportMode.Tram);
        departureInfo.addDueTram(dueTram);
        return departureInfo;
    }
}
