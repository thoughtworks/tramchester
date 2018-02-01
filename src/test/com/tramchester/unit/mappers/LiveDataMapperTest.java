package com.tramchester.unit.mappers;

import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.mappers.LiveDataMapper;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LiveDataMapperTest {

    public static String exampleData = "{\n" +
            "  \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n" +
            "    {\n" +
            "      \"Id\":1,\"Line\":\"Eccles\",\"TLAREF\":\"MEC\",\"PIDREF\":\"MEC-TPID03\",\"StationLocation\":\"MediaCityUK\",\"AtcoCode\":\"9400ZZMAMCU2\",\"Direction\":\"Incoming\",\"Dest0\":\"Piccadilly\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"1\",\"Dest1\":\"Piccadilly\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"12\",\"Dest2\":\"Piccadilly\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"21\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and services are expected to be busier than usual. Please plan your journey ahead with additional time for travel.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T11:45:00Z\"\n" +
            "    },{\n" +
            "      \"Id\":234,\"Line\":\"Airport\",\"TLAREF\":\"AIR\",\"PIDREF\":\"AIR-TPID01\",\"StationLocation\":\"Manchester Airport\",\"AtcoCode\":\"9400ZZMAAIR1\",\"Direction\":\"Incoming\",\"Dest0\":\"Deansgate Castlefield\",\"Carriages0\":\"Single\",\"Status0\":\"Due\",\"Wait0\":\"5\",\"Dest1\":\"Deansgate Castlefield\",\"Carriages1\":\"Single\",\"Status1\":\"Due\",\"Wait1\":\"17\",\"Dest2\":\"See Tram Front\",\"Carriages2\":\"Single\",\"Status2\":\"Due\",\"Wait2\":\"29\",\"Dest3\":\"\",\"Carriages3\":\"\",\"Status3\":\"\",\"MessageBoard\":\"Due to a signalling issue at Deansgate Airport Services will be running Airport to Cornbrook.Metrolink apologises for any inconvenience.\",\"Wait3\":\"\",\"LastUpdated\":\"2017-11-29T11:45:00Z\"\n" +
            "    }" +
            "]\n }\n";

    @Test
    public void shouldMapLiveDataToStationInfo() throws ParseException {
        LiveDataMapper mapper = new LiveDataMapper();

        List<StationDepartureInfo> info = mapper.map(exampleData);

        assertEquals(2, info.size());

        StationDepartureInfo departureInfoA = info.get(0);
        assertEquals("Eccles", departureInfoA.getLineName());
        assertEquals("9400ZZMAMCU2", departureInfoA.getStationPlatform());
        assertEquals( "MediaCityUK", departureInfoA.getLocation());
        assertEquals("Today Manchester City welcome Southampton at the Etihad Stadium KO is at 20:00 and " +
                "services are expected to be busier than usual. Please plan your journey " +
                "ahead with additional time for travel.", departureInfoA.getMessage());

        List<DueTram> dueTrams = departureInfoA.getDueTrams();
        assertEquals(3, dueTrams.size());
        DueTram dueTram = dueTrams.get(1);

        assertEquals("Piccadilly", dueTram.getDestination());
        assertEquals("Due", dueTram.getStatus());
        assertEquals(12, dueTram.getWait());
        assertEquals("Single",dueTram.getCarriages());

        DateTime expectedDate = new DateTime(2017, 11, 29,
                11, 45, 00, ISOChronology.getInstanceUTC());
        assertEquals(expectedDate, departureInfoA.getLastUpdate());

        StationDepartureInfo departureInfoB = info.get(1);
        assertEquals("Airport", departureInfoB.getLineName());
    }
}
