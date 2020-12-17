package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.AgencyData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgencyDataParserTest extends ParserTestHelper<AgencyData> {

    @BeforeEach
    void beforeEach() {
        super.before(AgencyData.class, "agency_id,agency_name,agency_url,agency_timezone,agency_lang");
    }

    @Test
    void shouldParseAnAgency() {
        AgencyData agencyData = parse("GMS,Stagecoach Manchester,http://www.tfgm.com,Europe/London,en");

        assertEquals("GMS", agencyData.getId());
        assertEquals("Stagecoach Manchester", agencyData.getName());
    }
}
