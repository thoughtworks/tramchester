package com.tramchester.unit.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.metrics.CacheMetrics;
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

        station = TramStations.Shudehill.fakeWithPlatform("someId1", Shudehill.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());
        platform = TestEnv.onlyPlatform(station);
    }

    @Test
    void shouldUpdateMessageCacheAndFetch() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, station, "some message", lastUpdate);
        departureInfoA.setStationPlatform(platform);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, Altrincham.fake(),
                "some different message", lastUpdate);
        departureInfoB.setStationPlatform(TestEnv.createPlatformFor(Altrincham.fake(), "someOther"));
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.numberOfEntries());

        TramDate date = TramDate.from(lastUpdate);
        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), date, updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());

        Optional<PlatformMessage> noMessage = repository.messagesFor(StringIdFor.createId("XXXX"), date, updateTime);
        assertTrue(noMessage.isEmpty());

        Optional<PlatformMessage> otherMessage = repository.messagesFor(StringIdFor.createId("someOther"), date, updateTime);
        assertTrue(otherMessage.isPresent());
        assertEquals("some different message", otherMessage.get().getMessage());

        List<PlatformMessage> stationMessages = repository.messagesFor(station, date, updateTime);
        assertEquals(1, stationMessages.size());
        assertEquals("some message", stationMessages.get(0).getMessage());

//        final Platform platform = MutablePlatform.buildForTFGMTram("XXXX", "platform name", Ashton.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station otherStation = TramStations.Ashton.fakeWithPlatform("XXXX", Ashton.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());

        List<PlatformMessage> noStationMsg = repository.messagesFor(otherStation, date, updateTime);
        assertTrue(noStationMsg.isEmpty());
    }

    @Test
    void shouldIgnoreAPIProvidedEmptyMessage() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, station, "<no message>", lastUpdate);
        departureInfo.setStationPlatform(platform);
        infos.add(departureInfo);

//        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(0, repository.numberOfEntries());
    }

    @Test
    void shouldIgnorEmptyMessage() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andReturn(true);
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, station, "", lastUpdate);
        departureInfo.setStationPlatform(platform);
        infos.add(departureInfo);

        //EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(0, repository.numberOfEntries());
    }

    @Test
    void shouldGiveNoMessagesIfNoRefresh() {
        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate),
                TramTime.ofHourMins(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOutOfDateRefresh() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, station, "some msg", lastUpdate);
        departureInfo.setStationPlatform(platform);
        infos.add(departureInfo);

//        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate().minusDays(1));
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate).minusDays(1));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate),
                TramTime.ofHourMins(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOldRefresh() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();
        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", Lines.Eccles,
                LineDirection.Incoming, station, "some msg", lastUpdate.minusMinutes(30));
        departureInfo.setStationPlatform(platform);
        infos.add(departureInfo);

//        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate),
                TramTime.ofHourMins(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreDuplicateUpdatesForPlatforms() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("123", Lines.Eccles,
                LineDirection.Incoming, station, "some message", lastUpdate);
        departureInfoA.setStationPlatform(platform);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("456", Lines.Eccles,
                LineDirection.Incoming, station, "some other message", lastUpdate);
        departureInfoB.setStationPlatform(platform);

        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        //EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());
    }

    @Test
    void shouldIgnoreEmptyDuplicateUpdatesForPlatforms() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();
        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("123", Lines.Eccles,
                LineDirection.Incoming, station, "some message", lastUpdate);
        departureInfoA.setStationPlatform(platform);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("456", Lines.Airport,
                LineDirection.Incoming, station, "", lastUpdate);
        departureInfoB.setStationPlatform(platform);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
//        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());
    }

    @Test
    void noNothingIfLiveDataIsDisabled() {

        TramTime time = TramTime.of(14, 34);

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(false);
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        TramDate date = providesNow.getTramDate();

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
