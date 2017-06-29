package com.tramchester.unit.dataimport.parsers;


import com.tramchester.dataimport.data.AgencyData;
import com.tramchester.dataimport.parsers.AgencyDataParser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AgencyDataParserTest {
    String data = "agency_id,agency_name,agency_url,agency_timezone,agency_lang";

    @Test
    public void shouldParseSingleLineOfData() {
        AgencyDataParser parser = new AgencyDataParser();
        AgencyData results = parser.parseEntry(data.split(","));

        assertThat(results.getId()).isEqualTo("agency_id");
        assertThat(results.getName()).isEqualTo("agency_name");
        assertThat(results.getUrl()).isEqualTo("agency_url");
    }

}
