package com.tramchester.unit.dataimport.naptan;

import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.unit.dataimport.ParserTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StopsDataTest extends ParserTestHelper<StopsData> {

    @BeforeEach
    void beforeEach() {
        super.before(StopsData.class, "\"ATCOCode\",\"NaptanCode\",\"PlateCode\",\"CleardownCode\",\"CommonName\",\"" +
                "CommonNameLang\",\"ShortCommonName\",\"ShortCommonNameLang\",\"Landmark\",\"LandmarkLang\",\"Street\",\"" +
                "StreetLang\",\"Crossing\",\"CrossingLang\",\"Indicator\",\"IndicatorLang\",\"Bearing\",\"NptgLocalityCode\",\"" +
                "LocalityName\",\"ParentLocalityName\",\"GrandParentLocalityName\",\"Town\",\"TownLang\",\"Suburb\",\"SuburbLang\",\"" +
                "LocalityCentre\",\"GridType\",\"Easting\",\"Northing\",\"Longitude\",\"Latitude\",\"StopType\",\"BusStopType\",\"" +
                "TimingStatus\",\"DefaultWaitTime\",\"Notes\",\"NotesLang\",\"AdministrativeAreaCode\",\"CreationDateTime\",\"" +
                "ModificationDateTime\",\"RevisionNumber\",\"Modification\",\"Status\"");
    }

    @Test
    void shouldParseData() {
        StopsData data = super.parse("\"9400ZZMAWWD2\",\"mantwgdp\",\"\",\"\",\"Westwood (Manchester Metrolink)\",\"\",\"\",\"\",\"\",\"\",\"" +
                "Middleton Road\",\"\",\"\",\"\",\"\",\"\",\"\",\"E0029527\",\"Westwood\",\"Chadderton\",\"\",\"\",\"\",\"\",\"\",\"1\",\"U\"," +
                "391756,405079,-2.1258738044,53.5422887902,\"PLT\",\"\",\"\",\"\",\"\",\"\",\"147\",\"2013-12-17T14:30:00\",\"2013-12-17T14:31:00\"," +
                "1,\"rev\",\"act\"");

        assertEquals("9400ZZMAWWD2", data.getAtcoCode());
        assertEquals("mantwgdp", data.getNaptanCode());
        assertEquals("Westwood", data.getLocalityName());
        assertEquals("Chadderton", data.getParentLocalityName());
    }
}
