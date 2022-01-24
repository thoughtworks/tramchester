package com.tramchester.unit.dataimport.naptan;

import com.tramchester.dataimport.NaPTAN.csv.NaptanStopCSVData;
import com.tramchester.geo.GridPosition;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.repository.naptan.NaptanStopType.busCoachTrolleyStationBay;
import static com.tramchester.repository.naptan.NaptanStopType.tramMetroUndergroundPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NaptanStopCSVDataTest extends ParserTestCSVHelper<NaptanStopCSVData> {

    @BeforeEach
    void beforeEach() {
        super.before(NaptanStopCSVData.class, "\"ATCOCode\",\"NaptanCode\",\"PlateCode\",\"CleardownCode\",\"CommonName\",\"" +
                "CommonNameLang\",\"ShortCommonName\",\"ShortCommonNameLang\",\"Landmark\",\"LandmarkLang\",\"Street\",\"" +
                "StreetLang\",\"Crossing\",\"CrossingLang\",\"Indicator\",\"IndicatorLang\",\"Bearing\",\"NptgLocalityCode\",\"" +
                "LocalityName\",\"ParentLocalityName\",\"GrandParentLocalityName\",\"Town\",\"TownLang\",\"Suburb\",\"SuburbLang\",\"" +
                "LocalityCentre\",\"GridType\",\"Easting\",\"Northing\",\"Longitude\",\"Latitude\",\"StopType\",\"BusStopType\",\"" +
                "TimingStatus\",\"DefaultWaitTime\",\"Notes\",\"NotesLang\",\"AdministrativeAreaCode\",\"CreationDateTime\",\"" +
                "ModificationDateTime\",\"RevisionNumber\",\"Modification\",\"Status\"");
    }

    @Test
    void shouldParseData() {
        NaptanStopCSVData data = super.parse("\"9400ZZMAWWD2\",\"mantwgdp\",\"\",\"\",\"Westwood (Manchester Metrolink)\",\"\",\"\",\"\",\"\",\"\",\"" +
                "Middleton Road\",\"\",\"\",\"\",\"\",\"\",\"\",\"E0029527\",\"Westwood\",\"Chadderton\",\"\",\"\",\"\",\"\",\"\",\"1\",\"U\"," +
                "391756,405079,-2.1258738044,53.5422887902,\"PLT\",\"\",\"\",\"\",\"\",\"\",\"147\",\"2013-12-17T14:30:00\",\"2013-12-17T14:31:00\"," +
                "1,\"rev\",\"act\"");

        assertEquals("9400ZZMAWWD2", data.getAtcoCode());
        assertEquals("mantwgdp", data.getNaptanCode());
        assertEquals("Westwood", data.getSuburb());
        assertEquals("Chadderton", data.getTown());
        assertEquals(new GridPosition(391756,405079), data.getGridPosition());
        assertEquals(tramMetroUndergroundPlatform, data.getStopType());
        assertEquals("E0029527", data.getNptgLocality());
    }

    @Test
    void shouldParseForBusStationWithIndicator() {
        NaptanStopCSVData data = super.parse("\"2800S16001B\",\"mergjtpm\",\"\",\"\",\"St Helens Bus Station\",\"en\",\"St Helens Bus St 2\",\"en\",\"" +
                "Bus Station\",\"en\",\"St Helens Bus Station\",\"en\",\"\",\"\",\"Stand 2\",\"en\",\"\",\"E0057858\",\"" +
                "St Helens\",\"\",\"\",\"St Helens\",\"en\",\"St Helens\",\"en\",\"1\",\"U\",351424,395440,-2.7329610659,53.4534662590,\"" +
                "BCS\",\"\",\"\",\"\",\"No Notes\",\"en\",\"090\",\"1969-12-31T00:00:00\",\"1969-12-31T23:00:00\",0,\"new\",\"act\"");

        assertEquals("2800S16001B", data.getAtcoCode());
        assertEquals("mergjtpm", data.getNaptanCode());
        assertEquals("St Helens Bus Station", data.getCommonName());
        assertEquals("St Helens", data.getSuburb());
        assertEquals("St Helens", data.getTown());
        assertEquals("Stand 2", data.getIndicator());
        assertEquals(busCoachTrolleyStationBay, data.getStopType());
    }
}
