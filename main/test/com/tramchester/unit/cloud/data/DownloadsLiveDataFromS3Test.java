package com.tramchester.unit.cloud.data;

import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.livedata.cloud.DownloadsLiveDataFromS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.tfgm.StationDepartureInfo;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.testSupport.reference.TramStations;
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

class DownloadsLiveDataFromS3Test extends EasyMockSupport {

    private LiveDataClientForS3 clientForS3;
    private DownloadsLiveDataFromS3 downloader;
    private S3Keys s3Keys;
    private StationDepartureInfoDTO departsDTO;

    private Capture<LiveDataClientForS3.ResponseMapper<StationDepartureInfoDTO>> responseMapperCapture;

    @BeforeEach
    void beforeEachTestRuns() {
        clientForS3 = createStrictMock(LiveDataClientForS3.class);
        StationDepartureMapper stationDepartureMapper = createStrictMock(StationDepartureMapper.class);
        s3Keys = createMock(S3Keys.class);

        downloader = new DownloadsLiveDataFromS3(clientForS3, stationDepartureMapper, s3Keys);

        StationDepartureInfo stationDepartureInfo = LiveDataUpdaterTest.createDepartureInfoWithDueTram(
                LocalDateTime.parse("2018-11-15T15:06:32"), "displayId",
                "platforId", "messageTxt", TramStations.NavigationRoad.fake());
        departsDTO = new StationDepartureInfoDTO(stationDepartureInfo);

        responseMapperCapture = Capture.newInstance();
    }

    @Test
    void shouldDownloadDataForGivenRange() throws S3Keys.S3KeyException {

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,42);
        Duration duration = Duration.of(1, HOURS);

        Set<String> keysFromS3 = Collections.singleton("keysFromS3");

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn("expectedPrefix");
        EasyMock.expect(s3Keys.parse("keysFromS3")).andReturn(start);

        EasyMock.expect(clientForS3.getKeysFor("expectedPrefix")).andReturn(keysFromS3);
        EasyMock.expect(clientForS3.downloadAndMap(EasyMock.eq(keysFromS3),
                EasyMock.capture(responseMapperCapture))).andReturn(Stream.of(departsDTO));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(departsDTO, results.get(0));
    }

    @Test
    void shouldDownloadDataForGivenRangeMultipleKeys() throws S3Keys.S3KeyException {

        StationDepartureInfo other = LiveDataUpdaterTest.createDepartureInfoWithDueTram(LocalDateTime.parse("2018-11-15T15:06:54"), "displayIdB",
                "platforIdB", "messageTxt", TramStations.Bury.fake());
        StationDepartureInfoDTO otherDTO = new StationDepartureInfoDTO(other);

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,1);
        Duration duration = Duration.of(1, HOURS);

        String keyA = "keyA";
        String keyB = "keyB";
        String keyC = "keyC";

        Set<String> keys = new HashSet<>();
        keys.add(keyA);
        keys.add(keyB);
        keys.add(keyC);

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn("expectedPrefix");
        EasyMock.expect(s3Keys.parse("keyA")).andReturn(start.plusMinutes(5));
        EasyMock.expect(s3Keys.parse("keyB")).andReturn(start.plusMinutes(10));
        EasyMock.expect(s3Keys.parse("keyC")).andReturn(start.plusMinutes(65));

        EasyMock.expect(clientForS3.getKeysFor("expectedPrefix")).andReturn(keys);

        Set<String> matching = new HashSet<>();
        matching.add(keyA);
        matching.add(keyB);
        EasyMock.expect(clientForS3.downloadAndMap(EasyMock.eq(matching), EasyMock.capture(responseMapperCapture))).
                andReturn(Stream.of(departsDTO, otherDTO));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(2, results.size());
        assertEquals(departsDTO, results.get(0));
        assertEquals(otherDTO, results.get(1));
    }

    @Test
    void shouldDownloadDataForGivenMutipleDays() throws S3Keys.S3KeyException {

        StationDepartureInfo other = LiveDataUpdaterTest.createDepartureInfoWithDueTram(LocalDateTime.parse("2018-11-15T15:06:54"),
                "displayIdB", "platforIdB", "messageTxt", TramStations.Bury.fake());
        StationDepartureInfoDTO otherDTO = new StationDepartureInfoDTO(other);

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,1);
        Duration duration = Duration.of(2, DAYS);

        String expectedPrefixA = "expectedPrefixA";
        String expectedPrefixB = "expectedPrefixB";
        String expectedPrefixC = "expectedPrefixC";

        String keyA = "keyA";
        String keyC = "keyC";

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn(expectedPrefixA);
        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate().plusDays(1))).andReturn(expectedPrefixB);
        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate().plusDays(2))).andReturn(expectedPrefixC);

        EasyMock.expect(s3Keys.parse(keyA)).andReturn(start);
        EasyMock.expect(s3Keys.parse(keyC)).andReturn(start.plusDays(2));

        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixA)).andReturn(Collections.singleton(keyA));
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixB)).andReturn(Collections.emptySet());
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixC)).andReturn(Collections.singleton(keyC));

        Set<String> matching = new HashSet<>();
        matching.add(keyA);
        matching.add(keyC);
        EasyMock.expect(clientForS3.downloadAndMap(EasyMock.eq(matching), EasyMock.capture(responseMapperCapture))).
                andReturn(Stream.of(departsDTO, otherDTO));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(2, results.size());
        assertEquals(departsDTO, results.get(0));
        assertEquals(otherDTO, results.get(1));
    }

    @Test
    void shouldSkipOutOfRangeKey() throws S3Keys.S3KeyException {
        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,42);
        Duration duration = Duration.of(1, HOURS);

        String expectedKey = "keyA";

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn("expectedPrefix");
        EasyMock.expect(s3Keys.parse("keyA")).andReturn(start.plusMinutes(65));

        EasyMock.expect(clientForS3.getKeysFor("expectedPrefix")).andReturn(Collections.singleton(expectedKey));

        replayAll();
        List<StationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertTrue(results.isEmpty());
    }

}
