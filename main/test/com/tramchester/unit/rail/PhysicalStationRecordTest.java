package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhysicalStationRecordTest {

    @Test
    void shouldParseBasicRecordCorrectly() {
        String line = "A    DERBY                         2DRBY   DBY   DBY14362 63356 6";

        PhysicalStationRecord result = PhysicalStationRecord.parse(line);

        assertEquals("DERBY", result.getName());
        assertEquals("DRBY", result.getTiplocCode());
        assertEquals(4362, result.getEasting());
        assertEquals(3356, result.getNorthing());
        assertEquals(RailInterchangeType.Medium, result.getRailInterchangeType());
    }

    @Test
    void shouldParseRecordsWithMissingGridCorrectly() {
        String line = "A    BALLINASLOE (CIE              0CATZBSGBSG   BSG00000E00000 5";

        PhysicalStationRecord result = PhysicalStationRecord.parse(line);

        assertEquals("BALLINASLOE (CIE", result.getName());
        assertEquals("CATZBSG", result.getTiplocCode());
        assertEquals(Integer.MIN_VALUE, result.getEasting());
        assertEquals(Integer.MIN_VALUE, result.getNorthing());
        assertEquals(RailInterchangeType.None, result.getRailInterchangeType());

    }

    @Test
    void shouldParseFileSpec() {
        String line = "A                             FILE-SPEC=05 1.00 12/11/21 18.10.25   193";

        PhysicalStationRecord result = PhysicalStationRecord.parse(line);

        assertEquals("",result.getName());
        assertEquals(Integer.MAX_VALUE, result.getNorthing());
        assertEquals(Integer.MAX_VALUE, result.getEasting());
    }

}
