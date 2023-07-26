package com.tramchester.unit.cloud.data;

import com.tramchester.cloud.data.LiveDataClientForS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.cloud.DownloadsLiveDataFromS3;
import com.tramchester.livedata.domain.DTO.archived.ArchivedDepartureDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import com.tramchester.testSupport.reference.TramStations;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadsLiveDataFromS3Test extends EasyMockSupport {

    private LiveDataClientForS3 clientForS3;
    private DownloadsLiveDataFromS3 downloader;
    private S3Keys s3Keys;
    private ArchivedStationDepartureInfoDTO departsDTO;

    private Capture<LiveDataClientForS3.ResponseMapper<ArchivedStationDepartureInfoDTO>> responseMapperCapture;

    @BeforeEach
    void beforeEachTestRuns() {
        clientForS3 = createStrictMock(LiveDataClientForS3.class);
        StationDepartureMapper stationDepartureMapper = createStrictMock(StationDepartureMapper.class);
        s3Keys = createMock(S3Keys.class);

        downloader = new DownloadsLiveDataFromS3(clientForS3, stationDepartureMapper, s3Keys);

        List<ArchivedDepartureDTO> dueTrams = new ArrayList<>();
        departsDTO = new ArchivedStationDepartureInfoDTO("lineName",
                "platforId", "messageTxt", dueTrams, LocalDateTime.parse("2018-11-15T15:06:32"),
                "displayId", TramStations.NavigationRoad.getName());

        responseMapperCapture = Capture.newInstance();
    }

    @Test
    void shouldDownloadDataForGivenRange() throws S3Keys.S3KeyException {

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,42);
        Duration duration = Duration.of(1, HOURS);

        Stream<String> keysFromS3 = Stream.of("keysFromS3");

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn("expectedPrefix");
        EasyMock.expect(s3Keys.parse("keysFromS3")).andReturn(start);

        EasyMock.expect(clientForS3.getKeysFor("expectedPrefix")).andReturn(keysFromS3);
        Set<String> keysToSubmit = Collections.singleton("keysFromS3");
        EasyMock.expect(clientForS3.downloadAndMap(EasyMock.eq(keysToSubmit),
                EasyMock.capture(responseMapperCapture))).andReturn(Stream.of(departsDTO));

        replayAll();
        List<ArchivedStationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(departsDTO, results.get(0));
    }

    @Test
    void shouldDownloadDataForGivenRangeMultipleKeys() throws S3Keys.S3KeyException {

        List<ArchivedDepartureDTO> dueTrams = new ArrayList<>();
        ArchivedStationDepartureInfoDTO otherDTO = new ArchivedStationDepartureInfoDTO("lineNameB",
                "platforIdB", "messageTxt", dueTrams, LocalDateTime.parse("2018-11-15T15:06:54"),
                "displayIdB", TramStations.Bury.getName());

        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,1);
        Duration duration = Duration.of(1, HOURS);

        String keyA = "keyA";
        String keyB = "keyB";
        String keyC = "keyC";

        Stream<String> keys = Stream.of(keyA, keyB, keyC);

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn("expectedPrefix");
        EasyMock.expect(s3Keys.parse("keyA")).andReturn(start.plusMinutes(5));
        EasyMock.expect(s3Keys.parse("keyB")).andReturn(start.plusMinutes(10));
        EasyMock.expect(s3Keys.parse("keyC")).andReturn(start.plusMinutes(65));

        EasyMock.expect(clientForS3.getKeysFor("expectedPrefix")).andReturn(keys);

        Set<String> matching = new HashSet<>(Arrays.asList(keyA, keyB));

        EasyMock.expect(clientForS3.downloadAndMap(EasyMock.eq(matching), EasyMock.capture(responseMapperCapture))).
                andReturn(Stream.of(departsDTO, otherDTO));

        replayAll();
        List<ArchivedStationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(2, results.size());
        assertEquals("displayId", results.get(0).getDisplayId());
        assertEquals("displayIdB", results.get(1).getDisplayId());
    }

    @Test
    void shouldDownloadDataForGivenMutipleDays() throws S3Keys.S3KeyException {

        List<ArchivedDepartureDTO> dueTrams = new ArrayList<>();
        ArchivedStationDepartureInfoDTO otherDTO = new ArchivedStationDepartureInfoDTO("lineNameB",
                "platforIdB", "messageTxt", dueTrams, LocalDateTime.parse("2018-11-15T15:06:54"),
                "displayIdB", TramStations.Bury.getName());

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

        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixA)).andReturn(Stream.of(keyA));
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixB)).andReturn(Stream.empty());
        EasyMock.expect(clientForS3.getKeysFor(expectedPrefixC)).andReturn(Stream.of(keyC));

        Set<String> matching = new HashSet<>();
        matching.add(keyA);
        matching.add(keyC);
        EasyMock.expect(clientForS3.downloadAndMap(EasyMock.eq(matching), EasyMock.capture(responseMapperCapture))).
                andReturn(Stream.of(departsDTO, otherDTO));

        replayAll();
        List<ArchivedStationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertEquals(2, results.size());
        assertEquals(departsDTO.getDisplayId(), results.get(0).getDisplayId());
        assertEquals(otherDTO.getDisplayId(), results.get(1).getDisplayId());
    }

    @Test
    void shouldSkipOutOfRangeKey() throws S3Keys.S3KeyException {
        LocalDateTime start = LocalDateTime.of(2020,11,29, 15,42);
        Duration duration = Duration.of(1, HOURS);

        String expectedKey = "keyA";

        EasyMock.expect(s3Keys.createPrefix(start.toLocalDate())).andReturn("expectedPrefix");
        EasyMock.expect(s3Keys.parse("keyA")).andReturn(start.plusMinutes(65));

        EasyMock.expect(clientForS3.getKeysFor("expectedPrefix")).andReturn(Stream.of(expectedKey));

        replayAll();
        List<ArchivedStationDepartureInfoDTO>  results = downloader.downloadFor(start, duration).collect(Collectors.toList());
        verifyAll();

        assertTrue(results.isEmpty());
    }


}
