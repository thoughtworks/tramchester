package com.tramchester.unit.cloud.data;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.cloud.data.DownloadsLiveData;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import com.tramchester.unit.repository.LiveDataUpdaterTest;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.*;

class DownloadsLiveDataTest extends EasyMockSupport {

    private ClientForS3 clientForS3;
    private DownloadsLiveData downloader;
    private S3Keys s3Keys;
    private StationDepartureMapper stationDepartureMapper;
    private StationDepartureInfoDTO departsDTO;

    Capture<ClientForS3.ResponseMapper<StationDepartureInfoDTO>> responseMapperCapture;

    @BeforeEach
    void beforeEachTestRuns() {
        clientForS3 = createStrictMock(ClientForS3.class);
        stationDepartureMapper = createStrictMock(StationDepartureMapper.class);
        s3Keys = new S3Keys(TestEnv.GET());
        downloader = new DownloadsLiveData(clientForS3, stationDepartureMapper, s3Keys);

        StationDepartureInfo stationDepartureInfo = LiveDataUpdaterTest.createDepartureInfoWithDueTram(LocalDateTime.parse("2018-11-15T15:06:32"), "displayId",
                "platforId", "messageTxt", TramStations.of(TramStations.NavigationRoad));
        departsDTO = new StationDepartureInfoDTO(stationDepartureInfo);

        responseMapperCapture = Capture.newInstance();

    }

    @Test
    void shouldDownloadDataForGivenRange() {

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,42);
        Duration duration = Duration.of(1, HOURS);

        String expectedPrefix = s3Keys.createPrefix(start.toLocalDate());
        String expectedKey = s3Keys.create(start);
        Set<String> keys = Collections.singleton(expectedKey);
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefix)).andReturn(keys);
        EasyMock.expect(clientForS3.download(EasyMock.eq(keys), EasyMock.capture(responseMapperCapture))).andReturn(Stream.of(departsDTO));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(departsDTO, results.get(0));
    }

    @Test
    void shouldDownloadDataForGivenRangeMultipleKeys() {

        StationDepartureInfo other = LiveDataUpdaterTest.createDepartureInfoWithDueTram(LocalDateTime.parse("2018-11-15T15:06:54"), "displayIdB",
                "platforIdB", "messageTxt", TramStations.of(TramStations.Bury));
        StationDepartureInfoDTO otherDTO = new StationDepartureInfoDTO(other);

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,1);
        Duration duration = Duration.of(1, HOURS);

        String expectedPrefix = s3Keys.createPrefix(start.toLocalDate());
        String keyA = s3Keys.create(start.plusMinutes(5));
        String keyB = s3Keys.create(start.plusMinutes(10));
        String keyC = s3Keys.create(start.plusMinutes(65));

        Set<String> keys = new HashSet<>();
        keys.add(keyA);
        keys.add(keyB);
        keys.add(keyC);
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefix)).andReturn(keys);

        Set<String> matching = new HashSet<>();
        matching.add(keyA);
        matching.add(keyB);
        EasyMock.expect(clientForS3.download(EasyMock.eq(matching), EasyMock.capture(responseMapperCapture))).andReturn(Stream.of(departsDTO, otherDTO));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(2, results.size());
        assertEquals(departsDTO, results.get(0));
        assertEquals(otherDTO, results.get(1));
    }

    @Test
    void shouldDownloadDataForGivenMutipleDays() {

        StationDepartureInfo other = LiveDataUpdaterTest.createDepartureInfoWithDueTram(LocalDateTime.parse("2018-11-15T15:06:54"), "displayIdB",
                "platforIdB", "messageTxt", TramStations.of(TramStations.Bury));
        StationDepartureInfoDTO otherDTO = new StationDepartureInfoDTO(other);

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,1);
        Duration duration = Duration.of(2, DAYS);

        String expectedPrefixA = s3Keys.createPrefix(start.toLocalDate());
        String expectedPrefixB = s3Keys.createPrefix(start.toLocalDate().plusDays(1));
        String expectedPrefixC = s3Keys.createPrefix(start.toLocalDate().plusDays(2));

        String keyA = s3Keys.create(start);
        String keyC = s3Keys.create(start.plusDays(2));

        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixA)).andReturn(Collections.singleton(keyA));
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixB)).andReturn(Collections.emptySet());
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixC)).andReturn(Collections.singleton(keyC));

        Set<String> matching = new HashSet<>();
        matching.add(keyA);
        matching.add(keyC);
        EasyMock.expect(clientForS3.download(EasyMock.eq(matching), EasyMock.capture(responseMapperCapture))).andReturn(Stream.of(departsDTO, otherDTO));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(2, results.size());
        assertEquals(departsDTO, results.get(0));
        assertEquals(otherDTO, results.get(1));
    }

    @Test
    void shouldSkipOutOfRangeKey() {
        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,42);
        Duration duration = Duration.of(1, HOURS);

        String expectedPrefix = s3Keys.createPrefix(start.toLocalDate());
        String expectedKey = s3Keys.create(start.plusMinutes(65));
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefix)).andReturn(Collections.singleton(expectedKey));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertTrue(results.isEmpty());
    }

}
