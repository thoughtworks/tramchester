package com.tramchester.unit.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.LineDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.liveUpdates.PlatformMessage;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.PlatformMessageRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformMessageRepositoryTest  extends EasyMockSupport {

    private ProvidesNow providesNow;
    private PlatformMessageRepository repository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        repository = new PlatformMessageRepository(providesNow);

        lastUpdate = TestEnv.LocalNow();

        station = TramStations.of(TramStations.Shudehill);
        platform = new Platform("someId1", "Shudehill platform 1");
        station.addPlatform(platform);

    }

    @Test
    void shouldUpdateMessageCacheAndFetch() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfoA = new StationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some message", lastUpdate);
        StationDepartureInfo departureInfoB = new StationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, IdFor.createId("someOther"), TramStations.of(Altrincham),
                "some different message", lastUpdate);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.of(lastUpdate);
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());

        Optional<PlatformMessage> noMessage = repository.messagesFor(IdFor.createId("XXXX"), lastUpdate.toLocalDate(), updateTime);
        assertTrue(noMessage.isEmpty());

        Optional<PlatformMessage> otherMessage = repository.messagesFor(IdFor.createId("someOther"), lastUpdate.toLocalDate(), updateTime);
        assertTrue(otherMessage.isPresent());
        assertEquals("some different message", otherMessage.get().getMessage());

        List<PlatformMessage> stationMessages = repository.messagesFor(station, lastUpdate.toLocalDate(), updateTime);
        assertEquals(1, stationMessages.size());
        assertEquals("some message", stationMessages.get(0).getMessage());

        Station otherStation = TramStations.of(TramStations.Ashton);
        otherStation.addPlatform(new Platform("XXXX", "platform name"));

        List<PlatformMessage> noStationMsg = repository.messagesFor(otherStation, lastUpdate.toLocalDate(), updateTime);
        assertTrue(noStationMsg.isEmpty());
    }

    @Test
    void shouldIgnoreAPIProvidedEmptyMessage() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfo = new StationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "<no message>", lastUpdate);
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(0, repository.numberOfEntries());
    }

    @Test
    void shouldIgnorEmptyMessage() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfo = new StationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "", lastUpdate);
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(0, repository.numberOfEntries());
    }

    @Test
    void shouldGiveNoMessagesIfNoRefresh() {
        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(), TramTime.of(lastUpdate));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOutOfDateRefresh() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfo = new StationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some msg", lastUpdate);
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate().minusDays(1));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(),
                TramTime.of(lastUpdate));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOldRefresh() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfo = new StationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some msg", lastUpdate.minusMinutes(30));
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(),
                TramTime.of(lastUpdate));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreDuplicateUpdatesForPlatforms() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfoA = new StationDepartureInfo("123", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some message", lastUpdate);
        StationDepartureInfo departureInfoB = new StationDepartureInfo("456", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some other message", lastUpdate);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.of(lastUpdate);
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());
    }

    @Test
    void shouldIgnoreEmptyDuplicateUpdatesForPlatforms() {
        List<StationDepartureInfo> infos = new LinkedList<>();

        StationDepartureInfo departureInfoA = new StationDepartureInfo("123", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some message", lastUpdate);
        StationDepartureInfo departureInfoB = new StationDepartureInfo("456", Lines.Airport,
                LineDirection.Incoming, platform.getId(), station,
                "", lastUpdate);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.of(lastUpdate);
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());
    }

}
