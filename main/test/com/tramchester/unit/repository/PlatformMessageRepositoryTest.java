package com.tramchester.unit.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformMessageRepositoryTest  extends EasyMockSupport {

    private ProvidesNow providesNow;
    private PlatformMessageRepository repository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;
    private TramchesterConfig config;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        config = createMock(TramchesterConfig.class);

        repository = new PlatformMessageRepository(providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()), config);

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        platform = MutablePlatform.buildForTFGMTram("someId1", "Shudehill platform 1", Shudehill.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        station = TramStations.Shudehill.fakeWith(platform);
    }

    @Test
    void shouldUpdateMessageCacheAndFetch() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some message", lastUpdate);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, StringIdFor.createId("someOther"), Altrincham.fake(),
                "some different message", lastUpdate);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.of(lastUpdate.toLocalTime());
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());

        Optional<PlatformMessage> noMessage = repository.messagesFor(StringIdFor.createId("XXXX"), lastUpdate.toLocalDate(), updateTime);
        assertTrue(noMessage.isEmpty());

        Optional<PlatformMessage> otherMessage = repository.messagesFor(StringIdFor.createId("someOther"), lastUpdate.toLocalDate(), updateTime);
        assertTrue(otherMessage.isPresent());
        assertEquals("some different message", otherMessage.get().getMessage());

        List<PlatformMessage> stationMessages = repository.messagesFor(station, lastUpdate.toLocalDate(), updateTime);
        assertEquals(1, stationMessages.size());
        assertEquals("some message", stationMessages.get(0).getMessage());

        final Platform platform = MutablePlatform.buildForTFGMTram("XXXX", "platform name", Ashton.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station otherStation = TramStations.Ashton.fakeWith(platform);

        List<PlatformMessage> noStationMsg = repository.messagesFor(otherStation, lastUpdate.toLocalDate(), updateTime);
        assertTrue(noStationMsg.isEmpty());
    }

    @Test
    void shouldIgnoreAPIProvidedEmptyMessage() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveDataEnabled()).andReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
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
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveDataEnabled()).andReturn(true);
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
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
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(),
                TramTime.of(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOutOfDateRefresh() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some msg", lastUpdate);
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate().minusDays(1));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(),
                TramTime.of(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOldRefresh() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();
        EasyMock.expect(config.liveDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some msg", lastUpdate.minusMinutes(30));
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), lastUpdate.toLocalDate(),
                TramTime.of(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreDuplicateUpdatesForPlatforms() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("123", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some message", lastUpdate);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("456", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some other message", lastUpdate);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.of(lastUpdate.toLocalTime());
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
        List<TramStationDepartureInfo> infos = new LinkedList<>();
        EasyMock.expect(config.liveDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("123", Lines.Eccles,
                LineDirection.Incoming, platform.getId(), station,
                "some message", lastUpdate);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("456", Lines.Airport,
                LineDirection.Incoming, platform.getId(), station,
                "", lastUpdate);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.of(lastUpdate.toLocalTime());
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
    void noNothingIfLiveDataIsDisabled() {
        LocalDate date = LocalDate.now();
        TramTime time = TramTime.of(14, 34);

        EasyMock.expect(config.liveDataEnabled()).andStubReturn(false);

        replayAll();
        List<PlatformMessage> messageForStation = repository.messagesFor(station, date, time);
        Optional<PlatformMessage> messageForPlatform = repository.messagesFor(StringIdFor.createId("platformId"), date, time);
        Set<Station> stationsWithMessages = repository.getStationsWithMessages(LocalTime.now());
        int numberOfEntries = repository.numberOfEntries();
        int numberStationsWithEntries = repository.numberStationsWithMessages(LocalDateTime.now());
        verifyAll();

        assertTrue(messageForStation.isEmpty());
        assertTrue(messageForPlatform.isEmpty());
        assertTrue(stationsWithMessages.isEmpty());
        assertEquals(0, numberOfEntries);
        assertEquals(0, numberStationsWithEntries);
    }

}
