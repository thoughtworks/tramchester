package com.tramchester.unit.repository;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataHTTPFetcher;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.livedata.tfgm.DueTramsRepository;
import com.tramchester.livedata.tfgm.LiveDataUpdater;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
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

import static com.tramchester.testSupport.reference.TramStations.*;

public class LiveDataUpdaterTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataUpdater repository;
    private ProvidesNow providesNow;
    private LocalDateTime lastUpdate;
    private DueTramsRepository dueTramsRepository;
    private PlatformMessageRepository platformMessageRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        providesNow = createMock(ProvidesNow.class);
        platformMessageRepository = createMock(PlatformMessageRepository.class);
        dueTramsRepository = createMock(DueTramsRepository.class);

        repository = new LiveDataUpdater(platformMessageRepository, dueTramsRepository, fetcher, mapper, providesNow);

        lastUpdate = TestEnv.LocalNow();
    }

    @Test
    void shouldUpdateRepositories() {
        List<StationDepartureInfo> info = new LinkedList<>();

        info.add(createDepartureInfoWithDueTram(lastUpdate, "yyy", "platformIdA",
                "some message", Altrincham.fake()));
        info.add(createDepartureInfoWithDueTram(lastUpdate, "303", "platformIdB",
                "some message", Altrincham.fake()));

        EasyMock.expect(providesNow.getNowHourMins()).andStubReturn(TramTime.ofHourMins(lastUpdate.toLocalTime()));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        EasyMock.expect(platformMessageRepository.updateCache(info)).andReturn(2);
        EasyMock.expect(dueTramsRepository.updateCache(info)).andReturn(2);

        replayAll();
        repository.refreshRespository();
        verifyAll();
    }

    @Test
    void shouldUpdateRepositoriesIgnoringStaleData() {
        List<StationDepartureInfo> info = new LinkedList<>();

        Station station = Altrincham.fake();
        StationDepartureInfo departureInfo = createDepartureInfoWithDueTram(lastUpdate, "yyy", "platformIdA",
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

        List<StationDepartureInfo> expected = Collections.singletonList(departureInfo);
        EasyMock.expect(platformMessageRepository.updateCache(expected)).andReturn(1);
        EasyMock.expect(dueTramsRepository.updateCache(expected)).andReturn(1);

        replayAll();
        repository.refreshRespository();
        verifyAll();
    }

    public static StationDepartureInfo createDepartureInfoWithDueTram(LocalDateTime lastUpdate,
                                                                      String displayId, String platformId, String message,
                                                                      Station location) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, Lines.Airport,
                LineDirection.Incoming, StringIdFor.createId(platformId), location, message, lastUpdate);
        DueTram dueTram = new DueTram(location, Bury.fake(),
                "Due", Duration.ofMinutes(42), "Single", lastUpdate.toLocalTime());
        departureInfo.addDueTram(dueTram);
        return departureInfo;
    }
}
