package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.AgencyData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgencyDataParserTest extends ParserTestCSVHelper<AgencyData> {

    @BeforeEach
    void beforeEach() {
        super.before(AgencyData.class, "agency_id,agency_name,agency_url,agency_timezone,agency_lang");
    }

    @Test
    void shouldParseAnAgency() {
        AgencyData agencyData = parse("GMS,Stagecoach Manchester,http://www.tfgm.com,Europe/London,en");

        assertEquals(Agency.createId("GMS"), agencyData.getId());
        assertEquals("Stagecoach Manchester", agencyData.getName());
    }
}
