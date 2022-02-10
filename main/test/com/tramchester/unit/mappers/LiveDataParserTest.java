package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.Lines;
import com.tramchester.livedata.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.mappers.LiveDataParser;
import com.tramchester.livedata.repository.TramStationByName;
import com.tramchester.repository.StationRepository;
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

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private TramStationByName tramStationByName;

    @BeforeEach
    void beforeEachTestRuns() {
        StationRepository stationRepository = createStrictMock(StationRepository.class);
        tramStationByName = createStrictMock(TramStationByName.class);
        parser = new LiveDataParser(tramStationByName, stationRepository);

        final Platform platformMC = MutablePlatform.buildForTFGMTram("9400ZZMAMCU2", "Media City Platform 2", MediaCityUK.getLatLong());
        Station mediaCity = MediaCityUK.fakeWith(platformMC);
//        mediaCity.addPlatform(platform);

        final Platform platformAirport = MutablePlatform.buildForTFGMTram("9400ZZMAAIR1", "Manchester Airport Platform 2",
                ManAirport.getLatLong());
        Station airport = ManAirport.fakeWith(platformAirport);
//        airport.addPlatform(platformAirport);

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
        EasyMock.expect(tramStationByName.getTramStationByName(station.getName())).andStubReturn(Optional.of(station.fake()));
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
            String line = String.format("""
                    {
                    "Id":%s,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK",
                    "AtcoCode":"9400ZZMAMCU2","Direction":"Incoming","Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due",
                    "Wait0":"1","Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12","Dest2":"Piccadilly",
                    "Carriages2":"Single","Status2":"Due","Wait2":"21","Dest3":"","Carriages3":"","Status3":"",
                    "MessageBoard":"Test.","Wait3":"","LastUpdated":"2017-11-29T%02d:45:00Z"
                        }""", i, i);
            message.append(line);
        }
        message.append(footer);

        replayAll();
        List<StationDepartureInfo> info = parser.parse(message.toString());
        assertEquals(11, info.size());
        for (int i = 1; i < 12; i++) {
            LocalDateTime expected = LocalDateTime.of(2017, 11, 29, i, 45);
            assertEquals(expected, info.get(i-1).getLastUpdate(), expected.toString());
        }
        verifyAll();
    }

    @Test
    void shouldMapLiveDataToStationInfo() {

        replayAll();
        List<StationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        assertEquals(2, info.size());

        StationDepartureInfo departureInfoA = info.get(0);
        assertEquals("1", departureInfoA.getDisplayId());
        assertEquals(Lines.Eccles, departureInfoA.getLine());
        assertEquals(StringIdFor.createId("9400ZZMAMCU2"), departureInfoA.getStationPlatform());
        assertEquals(MediaCityUK.getId(), departureInfoA.getStation().getId());
        assertEquals("Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and " +
                "services are expected to be busier than usual. Please plan your journey " +
                "ahead with additional time for travel.", departureInfoA.getMessage());
        assertEquals(LineDirection.Incoming, departureInfoA.getDirection());

        List<DueTram> dueTrams = departureInfoA.getDueTrams();
        assertEquals(3, dueTrams.size());
        DueTram dueTram = dueTrams.get(1);

        assertEquals("Piccadilly", dueTram.getDestination().getName());
        assertEquals("Due", dueTram.getStatus());
        assertMinutesEquals(12, dueTram.getWait());
        assertEquals("Single",dueTram.getCarriages());

        ZonedDateTime expectedDateA = ZonedDateTime.of(LocalDateTime.of(2017, 11, 29, 11, 45), TramchesterConfig.TimeZone);
        assertEquals(expectedDateA.toLocalDateTime(), departureInfoA.getLastUpdate());

        // WORKAROUND - Live data erronously gives timestamps as 'UTC'/'Z' even though they switch to DST/BST
        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals("234", departureInfoB.getDisplayId());

        assertEquals(Lines.Airport, departureInfoB.getLine());
        ZonedDateTime expectedDateB = ZonedDateTime.of(LocalDateTime.of(2017, 6, 29, 13, 55), TramchesterConfig.TimeZone);
        assertEquals(expectedDateB.toLocalDateTime(), departureInfoB.getLastUpdate());
        assertEquals(LineDirection.Incoming, departureInfoB.getDirection());
    }

    @Test
    void shouldNOTFilterOutPlatformsNotInTimetabledData() {

        // Turns out due trams are appearing, and for some single platform stations (i.e. nav road) the live data
        // does include 2 platforms.....

        String NoSuchMediaCityPlatform = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                    {
                      "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU5","Direction":"Incoming","Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due","Wait0":"1","Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12","Dest2":"Piccadilly","Carriages2":"Single","Status2":"Due","Wait2":"21","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                    },{
                      "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming","Dest0":"Deansgate Castlefield","Carriages0":"Single","Status0":"Due","Wait0":"5","Dest1":"Deansgate Castlefield","Carriages1":"Single","Status1":"Due","Wait1":"17","Dest2":"See Tram Front","Carriages2":"Single","Status2":"Due","Wait2":"29","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                    }]
                 }
                """;

        replayAll();
        List<StationDepartureInfo> info = parser.parse(NoSuchMediaCityPlatform);
        verifyAll();

        assertEquals(2, info.size());
    }

    @Test
    void shouldExcludeSeeTramFrontDestination()  {
        replayAll();
        List<StationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        assertEquals(2, info.size());
        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals(ManAirport.getId(), departureInfoB.getStation().getId());
        assertEquals(2, departureInfoB.getDueTrams().size());
    }

    @Test
    void shouldExcludeDueTramsWithDestinationSetToNotInService() {
        String notInService = exampleData.replaceFirst("Deansgate Castlefield", "Not in Service");

        replayAll();
        List<StationDepartureInfo> info = parser.parse(notInService);
        verifyAll();

        assertEquals(2, info.size());
        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals(ManAirport.getId(), departureInfoB.getStation().getId());
        assertEquals(1, departureInfoB.getDueTrams().size());
    }

    @Test
    void shouldParseDataWithDirectionIncomingOutgoing() {
        String bothDirections = exampleData.replaceAll("Incoming", "Incoming/Outgoing");

        replayAll();
        List<StationDepartureInfo> info = parser.parse(bothDirections);
        verifyAll();
        assertEquals(2, info.size());
        assertEquals(LineDirection.Both, info.get(0).getDirection());
        assertEquals(LineDirection.Both, info.get(1).getDirection());

    }

    @Test
    void shouldParseDestinationsThatIncludeVIAPostfixForDestination() {
        String exampleData = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                    {
                      "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming","Dest0":"Piccadilly Via Somewhere","Carriages0":"Single","Status0":"Due","Wait0":"1","Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12","Dest2":"Piccadilly","Carriages2":"Single","Status2":"Due","Wait2":"21","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                    },{
                      "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming","Dest0":"Deansgate Castlefield via Someplace","Carriages0":"Single","Status0":"Due","Wait0":"5","Dest1":"Deansgate Castlefield","Carriages1":"Single","Status1":"Due","Wait1":"17","Dest2":"See Tram Front","Carriages2":"Single","Status2":"Due","Wait2":"29","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                    }]
                 }
                """;

        replayAll();
        Assertions.assertAll(() -> parser.parse(exampleData));
        verifyAll();
    }

    @Test
    void shouldParseAshtonViaMCUK() {
        String exampleData = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                    {
                      "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming","Dest0":"Ashton Via MCUK","Carriages0":"Single","Status0":"Due","Wait0":"1","Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12","Dest2":"Piccadilly","Carriages2":"Single","Status2":"Due","Wait2":"21","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                    },{
                      "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming","Dest0":"Ashton via MCUK","Carriages0":"Single","Status0":"Due","Wait0":"5","Dest1":"Deansgate Castlefield","Carriages1":"Single","Status1":"Due","Wait1":"17","Dest2":"See Tram Front","Carriages2":"Single","Status2":"Due","Wait2":"29","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                    }]
                 }
                """;

        replayAll();
        Assertions.assertAll(() -> parser.parse(exampleData));
        verifyAll();
    }
}
