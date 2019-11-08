package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.parsers.RouteDataMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

public class RouteDataParserTest {
    private String routeA = "MET:MET4:O:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0";
    private String routeB = "MET:MET4:O:,XXX,MET4,Ashton-Under-Lyne - Manchester - Eccles,0";

    @Test
    public void shouldFilter() throws IOException {
        RouteDataMapper mapper = new RouteDataMapper(Collections.singleton("MET"));

        assertTrue(mapper.filter(ParserBuilder.getRecordFor(routeA)));
        assertFalse(mapper.filter(ParserBuilder.getRecordFor(routeB)));
    }

    @Test
    public void shouldParseRoute() throws IOException {
        RouteDataMapper routeDataParser = new RouteDataMapper(Collections.emptySet());

        RouteData result = routeDataParser.parseEntry(ParserBuilder.getRecordFor(routeA));

        assertThat(result.getId()).isEqualTo("MET:MET4:O:");
        assertThat(result.getCode()).isEqualTo("MET4");
        assertThat(result.getName()).isEqualTo("Ashton-Under-Lyne - Manchester - Eccles");
        assertThat(result.getAgency()).isEqualTo("MET");
    }

}