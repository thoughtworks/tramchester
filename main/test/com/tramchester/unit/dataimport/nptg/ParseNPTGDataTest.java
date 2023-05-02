package com.tramchester.unit.dataimport.nptg;

import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParseNPTGDataTest extends ParserTestCSVHelper<NPTGData> {

    @BeforeEach
    void beforeEachTestRuns() {
        String header = "ATCOCode,NaptanCode,PlateCode,CleardownCode,CommonName,CommonNameLang,ShortCommonName,ShortCommonNameLang,Landmark," +
                "LandmarkLang,Street,StreetLang,Crossing,CrossingLang,Indicator,IndicatorLang,Bearing,NptgLocalityCode,LocalityName," +
                "ParentLocalityName,GrandParentLocalityName,Town,TownLang,Suburb,SuburbLang,LocalityCentre,GridType,Easting,Northing," +
                "Longitude,Latitude,StopType,BusStopType,TimingStatus,DefaultWaitTime,Notes,NotesLang,AdministrativeAreaCode,CreationDateTime," +
                "ModificationDateTime,RevisionNumber,Modification,Status";
        super.before(NPTGData.class, header);
    }

    @Test
    void shouldParseCodeWithNoAdminArea() {
        String text = "1800SJ11291,MANJPWPG,,,Park Road,,Park Rd,,HOUSE 275,,Ashley Road,,,,nr,,S,N0077434,Ashley Heath," +
                "Altrincham,,,,,,false,UKOS,377283,386186,-2.342899,53.37204,BCT,MKD,OTH,,,,083," +
                "2014-10-09T00:00:00,2014-10-09T00:00:00,2,revise,active";

        NPTGData item = super.parse(text);
        assertEquals("1800SJ11291", item.getActoCode());
        assertEquals("Ashley Heath", item.getLocalityName());
        assertEquals("Altrincham", item.getParentLocalityName());
    }



}
