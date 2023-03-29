package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.StationByName;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
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
    private StationByName stationByName;
    private Platform platformMC;
    private PlatformRepository platformRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        StationRepository stationRepository = createStrictMock(StationRepository.class);
        stationByName = createStrictMock(StationByName.class);
        AgencyRepository agencyRepository = createMock(AgencyRepository.class);
        platformRepository = createMock(PlatformRepository.class);

        Station mediaCity = MediaCityUK.fakeWithPlatform("9400ZZMAMCU2", MediaCityUK.getLatLong(),
                DataSourceID.unknown, NaptanArea.invalidId());
        platformMC = TestEnv.findOnlyPlatform(mediaCity);

        Station airport = ManAirport.fakeWithPlatform("9400ZZMAAIR1",
                ManAirport.getLatLong(), DataSourceID.unknown, NaptanArea.invalidId());
        final Platform platformAirport = TestEnv.findOnlyPlatform(airport);

        EasyMock.expect(stationRepository.getStationById(MediaCityUK.getId())).andStubReturn(mediaCity);
        EasyMock.expect(stationRepository.getStationById(ManAirport.getId())).andStubReturn(airport);
        EasyMock.expect(stationRepository.hasStationId(MediaCityUK.getId())).andStubReturn(true);
        EasyMock.expect(stationRepository.hasStationId(ManAirport.getId())).andStubReturn(true);

        EasyMock.expect(platformRepository.hasPlatformId(platformMC.getId())).andStubReturn(true);
        EasyMock.expect(platformRepository.getPlatformById(platformMC.getId())).andStubReturn(platformMC);

        EasyMock.expect(platformRepository.hasPlatformId(platformAirport.getId())).andStubReturn(true);
        EasyMock.expect(platformRepository.getPlatformById(platformAirport.getId())).andStubReturn(platformAirport);

        expectationByName(Piccadilly);
        expectationByName(MediaCityUK);
        expectationByName(ManAirport);
        expectationByName(Deansgate);
        expectationByName(Ashton);

        EasyMock.expect(stationByName.getTramStationByName("See Tram Front")).andStubReturn(Optional.empty());
        EasyMock.expect(stationByName.getTramStationByName("")).andStubReturn(Optional.empty());

        EasyMock.expect(agencyRepository.get(MutableAgency.METL)).andStubReturn(TestEnv.MetAgency());

        parser = new LiveDataParser(stationByName, stationRepository, platformRepository, agencyRepository);


    }

    private void expectationByName(TramStations station) {
        EasyMock.expect(stationByName.getTramStationByName(station.getName())).andStubReturn(Optional.of(station.fake()));
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
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(message.toString());
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
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        assertEquals(2, info.size());

        TramStationDepartureInfo departureInfoA = info.get(0);
        assertEquals("1", departureInfoA.getDisplayId());
        assertEquals(Lines.Eccles, departureInfoA.getLine());
        assertEquals(platformMC, departureInfoA.getStationPlatform());
        assertEquals(MediaCityUK.getId(), departureInfoA.getStation().getId());
        assertEquals("Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and " +
                "services are expected to be busier than usual. Please plan your journey " +
                "ahead with additional time for travel.", departureInfoA.getMessage());
        assertEquals(LineDirection.Incoming, departureInfoA.getDirection());

        List<UpcomingDeparture> dueTrams = departureInfoA.getDueTrams();
        assertEquals(3, dueTrams.size());
        UpcomingDeparture dueTram = dueTrams.get(1);

        assertEquals("Piccadilly", dueTram.getDestination().getName());
        assertEquals("Due", dueTram.getStatus());
        //assertMinutesEquals(12, dueTram.getWait());
        assertEquals("Single",dueTram.getCarriages());

        ZonedDateTime expectedDateA = ZonedDateTime.of(LocalDateTime.of(2017, 11, 29, 11, 45), TramchesterConfig.TimeZoneId);
        assertEquals(expectedDateA.toLocalDateTime(), departureInfoA.getLastUpdate());

        assertEquals(departureInfoA.getLastUpdate().plusMinutes(12).toLocalTime(), dueTram.getWhen().asLocalTime());

        // WORKAROUND - Live data erronously gives timestamps as 'UTC'/'Z' even though they switch to DST/BST
        TramStationDepartureInfo departureInfoB = info.get(1);
        assertEquals("234", departureInfoB.getDisplayId());

        assertEquals(Lines.Airport, departureInfoB.getLine());
        ZonedDateTime expectedDateB = ZonedDateTime.of(LocalDateTime.of(2017, 6, 29, 13, 55), TramchesterConfig.TimeZoneId);
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

        PlatformId platformId = PlatformId.createId(MediaCityUK.getId(), "5");
        EasyMock.expect(platformRepository.hasPlatformId(platformId)).andReturn(false);

        replayAll();
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(NoSuchMediaCityPlatform);
        verifyAll();

        assertEquals(2, info.size());
    }

    @Test
    void shouldExcludeSeeTramFrontDestination()  {
        replayAll();
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(exampleData);
        verifyAll();

        assertEquals(2, info.size());
        TramStationDepartureInfo departureInfoB = info.get(1);
        assertEquals(ManAirport.getId(), departureInfoB.getStation().getId());
        assertEquals(2, departureInfoB.getDueTrams().size());
    }

    @Test
    void shouldExcludeDueTramsWithDestinationSetToNotInService() {
        String notInService = exampleData.replaceFirst("Deansgate Castlefield", "Not in Service");

        replayAll();
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(notInService);
        verifyAll();

        assertEquals(2, info.size());
        TramStationDepartureInfo departureInfoB = info.get(1);
        assertEquals(ManAirport.getId(), departureInfoB.getStation().getId());
        assertEquals(1, departureInfoB.getDueTrams().size());
    }

    @Test
    void shouldParseDataWithDirectionIncomingOutgoing() {
        String bothDirections = exampleData.replaceAll("Incoming", "Incoming/Outgoing");

        replayAll();
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(bothDirections);
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
        parser.start();
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
        parser.start();
        Assertions.assertAll(() -> parser.parse(exampleData));
        verifyAll();
    }
}
