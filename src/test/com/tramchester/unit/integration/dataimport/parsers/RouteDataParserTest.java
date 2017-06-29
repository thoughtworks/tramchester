package com.tramchester.integration.dataimport.parsers;

import com.tramchester.integration.dataimport.data.RouteData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RouteDataParserTest {
    private String route = "MET:MET4:O:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0";

    @Test
    public void shouldParseRoute() throws Exception {
        RouteDataParser routeDataParser = new RouteDataParser();
        RouteData result = routeDataParser.parseEntry(this.route.split(","));

        assertThat(result.getId()).isEqualTo("MET:MET4:O:");
        assertThat(result.getCode()).isEqualTo("MET4");
        assertThat(result.getName()).isEqualTo("Ashton-Under-Lyne - Manchester - Eccles");
        assertThat(result.getAgency()).isEqualTo("MET");
    }

}