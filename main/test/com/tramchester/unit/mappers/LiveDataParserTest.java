package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.StationRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class LiveDataParserTest extends EasyMockSupport {

    public static String exampleData = "{\n" +
            "  \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n" +
            "    {\n" +
            "      \"Id\":1,\"Line\":\"Eccles\",\"TLAREF\":\"MEC\",\"PIDREF\":\"MEC-TPID03\",\"StationLocation\":\"MediaCityUK\",\"AtcoCode\":\"9400ZZMAMCU2\",\"Direction\":\"Incoming\",\"Dest0\":\"Piccadilly\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"1\",\"Dest1\":\"Piccadilly\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"12\",\"Dest2\":\"Piccadilly\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"21\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T11:45:00Z\"\n" +
            "    },{\n" +
            "      \"Id\":234,\"Line\":\"Airport\",\"TLAREF\":\"AIR\",\"PIDREF\":\"AIR-TPID01\",\"StationLocation\":\"Manchester Airport\",\"AtcoCode\":\"9400ZZMAAIR1\",\"Direction\":\"Incoming\",\"Dest0\":\"Deansgate Castlefield\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"5\",\"Dest1\":\"Deansgate Castlefield\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"17\",\"Dest2\":\"See Tram Front\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"29\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-06-29T13:55:00Z\"\n" +
            "    }" +
            "]\n }\n";


    private LiveDataParser parser;
    private StationRepository stationRepository;

    @Before
    public void beforeEachTestRuns() {
        stationRepository = createMock(StationRepository.class);
        parser = new LiveDataParser(stationRepository);

        EasyMock.expect(stationRepository.getStation(Stations.MediaCityUK.getId())).andStubReturn(Optional.of(Stations.MediaCityUK));
        EasyMock.expect(stationRepository.getStation(Stations.ManAirport.getId())).andStubReturn(Optional.of(Stations.ManAirport));

        EasyMock.expect(stationRepository.getStationByName("Piccadilly")).andStubReturn(Optional.of(Stations.Piccadilly));
        EasyMock.expect(stationRepository.getStationByName("MediaCityUK")).andStubReturn(Optional.of(Stations.MediaCityUK));
        EasyMock.expect(stationRepository.getStationByName("Manchester Airport")).andStubReturn(Optional.of(Stations.ManAirport));


        EasyMock.expect(stationRepository.getStationByName("")).andStubReturn(Optional.empty());
        EasyMock.expect(stationRepository.getStationByName("Deansgate Castlefield")).andStubReturn(Optional.of(Stations.Deansgate));
        EasyMock.expect(stationRepository.getStationByName("See Tram Front")).andStubReturn(Optional.empty());
    }

    @Test
    public void shouldMapTimesCorrectlyDuringEarlyHours() throws ParseException {
        String header = "{\n \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n";
        String footer = "]\n }\n";

        StringBuilder message = new StringBuilder();
        message.append(header);
        for (int i = 1; i < 12; i++) {
            if (i>1) {
                message.append(",\n");
            }
            String line = String.format( "{\n" +
                    "\"Id\":%s,\"Line\":\"Eccles\",\"TLAREF\":\"MEC\",\"PIDREF\":\"MEC-TPID03\"," +
                    "\"StationLocation\":\"MediaCityUK\",\"AtcoCode\":\"9400ZZMAMCU2\",\"Direction\":" +
                    "\"Incoming\",\"Dest0\":\"Piccadilly\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"1\",\"" +
                    "Dest1\":\"Piccadilly\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"12\",\"" +
                    "Dest2\":\"Piccadilly\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"21\",\"" +
                    "Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"" +
                    "MessageBoard\":\"Test.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T%02d:45:00Z\"\n" +
                    "    }", i, i);
            message.append(line);
        }
        message.append(footer);

        replayAll();
        List<StationDepartureInfo> info = parser.parse(message.toString());
        assertEquals(11, info.size());
        for (int i = 1; i < 12; i++) {
            LocalDateTime expected = LocalDateTime.of(2017, 11, 29, i, 45);
            assertEquals(expected.toString(), expected, info.get(i-1).getLastUpdate());
        }
        verifyAll();
    }

    @Test
    public void shouldMapLiveDataToStationInfo() throws ParseException {

        replayAll();
        List<StationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        assertEquals(2, info.size());

        StationDepartureInfo departureInfoA = info.get(0);
        assertEquals("Eccles", departureInfoA.getLineName());
        assertEquals("9400ZZMAMCU2", departureInfoA.getStationPlatform());
        assertEquals("MediaCityUK", departureInfoA.getLocation());
        assertEquals("Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and " +
                "services are expected to be busier than usual. Please plan your journey " +
                "ahead with additional time for travel.", departureInfoA.getMessage());

        List<DueTram> dueTrams = departureInfoA.getDueTrams();
        assertEquals(3, dueTrams.size());
        DueTram dueTram = dueTrams.get(1);

        assertEquals("Piccadilly", dueTram.getDestination().getName());
        assertEquals("Due", dueTram.getStatus());
        assertEquals(12, dueTram.getWait());
        assertEquals("Single",dueTram.getCarriages());

        ZonedDateTime expectedDateA = ZonedDateTime.of(LocalDateTime.of(2017, 11, 29, 11, 45), TramchesterConfig.TimeZone);
        assertEquals(expectedDateA.toLocalDateTime(), departureInfoA.getLastUpdate());

        // WORKAROUND - Live data erronously gives timestamps as 'UTC'/'Z' even though they switch to DST/BST
        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals("Airport", departureInfoB.getLineName());
        ZonedDateTime expectedDateB = ZonedDateTime.of(LocalDateTime.of(2017, 06, 29, 13, 55), TramchesterConfig.TimeZone);
        assertEquals(expectedDateB.toLocalDateTime(), departureInfoB.getLastUpdate());
    }

    @Test
    public void shouldExcludeSeeTramFrontDestination() throws ParseException {
        replayAll();
        List<StationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        assertEquals(2, info.size());
        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals(Stations.ManAirport.getName(), departureInfoB.getLocation());
        assertEquals(2, departureInfoB.getDueTrams().size());
    }

    @Test
    public void shouldExcludeDueTramsWithDestinationSetToNotInService() throws ParseException {

        String notInService = exampleData.replaceFirst("Deansgate Castlefield", "Not in Service");
        replayAll();
        List<StationDepartureInfo> info = parser.parse(notInService);
        verifyAll();
        assertEquals(2, info.size());

        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals(Stations.ManAirport.getName(), departureInfoB.getLocation());
        assertEquals(1, departureInfoB.getDueTrams().size());

    }
}
