package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Platform;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.liveUpdates.Lines;
import com.tramchester.livedata.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.livedata.mappers.LiveDataParser;
import com.tramchester.repository.StationRepository;
import com.tramchester.livedata.repository.TramStationByName;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.reference.TramStations.*;

class LiveDataParserTest extends EasyMockSupport {

    private static final String exampleData = """
            {
              "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                {
                  "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming","Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due","Wait0":"1","Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12","Dest2":"Piccadilly","Carriages2":"Single","Status2":"Due","Wait2":"21","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                },{
                  "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming","Dest0":"Deansgate Castlefield","Carriages0":"Single","Status0":"Due","Wait0":"5","Dest1":"Deansgate Castlefield","Carriages1":"Single","Status1":"Due","Wait1":"17","Dest2":"See Tram Front","Carriages2":"Single","Status2":"Due","Wait2":"29","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                }]
             }
            """;


    private LiveDataParser parser;
    private StationRepository stationRepository;
    private TramStationByName tramStationByName;

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = createStrictMock(StationRepository.class);
        tramStationByName = createStrictMock(TramStationByName.class);
        parser = new LiveDataParser(tramStationByName, stationRepository);

        MutableStation mediaCity = of(MediaCityUK);
        mediaCity.addPlatform(new Platform("9400ZZMAMCU2", "Media City Platform 2", MediaCityUK.getLatLong()));

        MutableStation airport = of(ManAirport);
        airport.addPlatform(new Platform("9400ZZMAAIR1", "Manchester Airport Platform 2", ManAirport.getLatLong()));

        EasyMock.expect(stationRepository.getStationById(MediaCityUK.getId())).andStubReturn(mediaCity);
        EasyMock.expect(stationRepository.getStationById(ManAirport.getId())).andStubReturn(airport);
        EasyMock.expect(stationRepository.hasStationId(MediaCityUK.getId())).andStubReturn(true);
        EasyMock.expect(stationRepository.hasStationId(ManAirport.getId())).andStubReturn(true);

        expectationByName(Piccadilly);
        expectationByName(MediaCityUK);
        expectationByName(ManAirport);
        expectationByName(Deansgate);
        expectationByName(Ashton);

        EasyMock.expect(tramStationByName.getTramStationByName("See Tram Front")).andStubReturn(Optional.empty());
        EasyMock.expect(tramStationByName.getTramStationByName("")).andStubReturn(Optional.empty());

    }

    private void expectationByName(TramStations station) {
        EasyMock.expect(tramStationByName.getTramStationByName(station.getName())).andStubReturn(Optional.of(of(station)));
    }

    @Test
    void shouldMapTimesCorrectlyDuringEarlyHours() {
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
        Assertions.assertEquals(11, info.size());
        for (int i = 1; i < 12; i++) {
            LocalDateTime expected = LocalDateTime.of(2017, 11, 29, i, 45);
            Assertions.assertEquals(expected, info.get(i-1).getLastUpdate(), expected.toString());
        }
        verifyAll();
    }

    @Test
    void shouldMapLiveDataToStationInfo() {

        replayAll();
        List<StationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        Assertions.assertEquals(2, info.size());

        StationDepartureInfo departureInfoA = info.get(0);
        Assertions.assertEquals("1", departureInfoA.getDisplayId());
        Assertions.assertEquals(Lines.Eccles, departureInfoA.getLine());
        Assertions.assertEquals(StringIdFor.createId("9400ZZMAMCU2"), departureInfoA.getStationPlatform());
        Assertions.assertEquals(MediaCityUK.getId(), departureInfoA.getStation().getId());
        Assertions.assertEquals("Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and " +
                "services are expected to be busier than usual. Please plan your journey " +
                "ahead with additional time for travel.", departureInfoA.getMessage());
        Assertions.assertEquals(LineDirection.Incoming, departureInfoA.getDirection());

        List<DueTram> dueTrams = departureInfoA.getDueTrams();
        Assertions.assertEquals(3, dueTrams.size());
        DueTram dueTram = dueTrams.get(1);

        Assertions.assertEquals("Piccadilly", dueTram.getDestination().getName());
        Assertions.assertEquals("Due", dueTram.getStatus());
        Assertions.assertEquals(12, dueTram.getWait());
        Assertions.assertEquals("Single",dueTram.getCarriages());

        ZonedDateTime expectedDateA = ZonedDateTime.of(LocalDateTime.of(2017, 11, 29, 11, 45), TramchesterConfig.TimeZone);
        Assertions.assertEquals(expectedDateA.toLocalDateTime(), departureInfoA.getLastUpdate());

        // WORKAROUND - Live data erronously gives timestamps as 'UTC'/'Z' even though they switch to DST/BST
        StationDepartureInfo departureInfoB = info.get(1);
        Assertions.assertEquals("234", departureInfoB.getDisplayId());

        Assertions.assertEquals(Lines.Airport, departureInfoB.getLine());
        ZonedDateTime expectedDateB = ZonedDateTime.of(LocalDateTime.of(2017, 6, 29, 13, 55), TramchesterConfig.TimeZone);
        Assertions.assertEquals(expectedDateB.toLocalDateTime(), departureInfoB.getLastUpdate());
        Assertions.assertEquals(LineDirection.Incoming, departureInfoB.getDirection());
    }

    @Test
    void shouldNOTFilterOutPlatformsNotInTimetabledData() {

        // Turns out due trams are appearing, and for some single platform stations (i.e. nav road) the live data
        // does include 2 platforms.....

        String NoSuchMediaCityPlatform = "{\n" +
                "  \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n" +
                "    {\n" +
                "      \"Id\":1,\"Line\":\"Eccles\",\"TLAREF\":\"MEC\",\"PIDREF\":\"MEC-TPID03\",\"StationLocation\":\"MediaCityUK\",\"AtcoCode\":\"9400ZZMAMCU5\",\"Direction\":\"Incoming\",\"Dest0\":\"Piccadilly\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"1\",\"Dest1\":\"Piccadilly\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"12\",\"Dest2\":\"Piccadilly\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"21\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T11:45:00Z\"\n" +
                "    },{\n" +
                "      \"Id\":234,\"Line\":\"Airport\",\"TLAREF\":\"AIR\",\"PIDREF\":\"AIR-TPID01\",\"StationLocation\":\"Manchester Airport\",\"AtcoCode\":\"9400ZZMAAIR1\",\"Direction\":\"Incoming\",\"Dest0\":\"Deansgate Castlefield\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"5\",\"Dest1\":\"Deansgate Castlefield\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"17\",\"Dest2\":\"See Tram Front\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"29\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-06-29T13:55:00Z\"\n" +
                "    }" +
                "]\n }\n";

        replayAll();
        List<StationDepartureInfo> info = parser.parse(NoSuchMediaCityPlatform);
        verifyAll();

        Assertions.assertEquals(2, info.size());
    }

    @Test
    void shouldExcludeSeeTramFrontDestination()  {
        replayAll();
        List<StationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        Assertions.assertEquals(2, info.size());
        StationDepartureInfo departureInfoB = info.get(1);
        Assertions.assertEquals(ManAirport.getId(), departureInfoB.getStation().getId());
        Assertions.assertEquals(2, departureInfoB.getDueTrams().size());
    }

    @Test
    void shouldExcludeDueTramsWithDestinationSetToNotInService() {
        String notInService = exampleData.replaceFirst("Deansgate Castlefield", "Not in Service");

        replayAll();
        List<StationDepartureInfo> info = parser.parse(notInService);
        verifyAll();

        Assertions.assertEquals(2, info.size());
        StationDepartureInfo departureInfoB = info.get(1);
        Assertions.assertEquals(ManAirport.getId(), departureInfoB.getStation().getId());
        Assertions.assertEquals(1, departureInfoB.getDueTrams().size());
    }

    @Test
    void shouldParseDataWithDirectionIncomingOutgoing() {
        String bothDirections = exampleData.replaceAll("Incoming", "Incoming/Outgoing");

        replayAll();
        List<StationDepartureInfo> info = parser.parse(bothDirections);
        verifyAll();
        Assertions.assertEquals(2, info.size());
        Assertions.assertEquals(LineDirection.Both, info.get(0).getDirection());
        Assertions.assertEquals(LineDirection.Both, info.get(1).getDirection());

    }

    @Test
    void shouldParseDestinationsThatIncludeVIAPostfixForDestination() {
        String exampleData = "{\n" +
                "  \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n" +
                "    {\n" +
                "      \"Id\":1,\"Line\":\"Eccles\",\"TLAREF\":\"MEC\",\"PIDREF\":\"MEC-TPID03\",\"StationLocation\":\"MediaCityUK\",\"AtcoCode\":\"9400ZZMAMCU2\",\"Direction\":\"Incoming\",\"Dest0\":\"Piccadilly Via Somewhere\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"1\",\"Dest1\":\"Piccadilly\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"12\",\"Dest2\":\"Piccadilly\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"21\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T11:45:00Z\"\n" +
                "    },{\n" +
                "      \"Id\":234,\"Line\":\"Airport\",\"TLAREF\":\"AIR\",\"PIDREF\":\"AIR-TPID01\",\"StationLocation\":\"Manchester Airport\",\"AtcoCode\":\"9400ZZMAAIR1\",\"Direction\":\"Incoming\",\"Dest0\":\"Deansgate Castlefield via Someplace\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"5\",\"Dest1\":\"Deansgate Castlefield\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"17\",\"Dest2\":\"See Tram Front\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"29\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-06-29T13:55:00Z\"\n" +
                "    }" +
                "]\n }\n";

        replayAll();
        Assertions.assertAll(() -> parser.parse(exampleData));
        verifyAll();
    }

    @Test
    void shouldParseAshtonViaMCUK() {
        String exampleData = "{\n" +
                "  \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n" +
                "    {\n" +
                "      \"Id\":1,\"Line\":\"Eccles\",\"TLAREF\":\"MEC\",\"PIDREF\":\"MEC-TPID03\",\"StationLocation\":\"MediaCityUK\",\"AtcoCode\":\"9400ZZMAMCU2\",\"Direction\":\"Incoming\",\"Dest0\":\"Ashton Via MCUK\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"1\",\"Dest1\":\"Piccadilly\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"12\",\"Dest2\":\"Piccadilly\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"21\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T11:45:00Z\"\n" +
                "    },{\n" +
                "      \"Id\":234,\"Line\":\"Airport\",\"TLAREF\":\"AIR\",\"PIDREF\":\"AIR-TPID01\",\"StationLocation\":\"Manchester Airport\",\"AtcoCode\":\"9400ZZMAAIR1\",\"Direction\":\"Incoming\",\"Dest0\":\"Ashton via MCUK\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"5\",\"Dest1\":\"Deansgate Castlefield\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"17\",\"Dest2\":\"See Tram Front\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"29\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-06-29T13:55:00Z\"\n" +
                "    }" +
                "]\n }\n";

        replayAll();
        Assertions.assertAll(() -> parser.parse(exampleData));
        verifyAll();
    }
}
