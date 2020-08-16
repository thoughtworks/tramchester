package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.parsers.RouteDataMapper;
import com.tramchester.domain.IdFor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class RouteDataParserTest {
    private static final String routeA = "MET:MET4:O:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0";
    private static final String routeB = "MET:MET4:O:,XXX,MET4,Ashton-Under-Lyne - Manchester - Eccles,0";

    private static final String problemBusRoute = "JSC: 588:C:,JSC, 588,\"Leigh - Lowton, Scott Road\",3";

    @Test
    void shouldFilter() throws IOException {
        RouteDataMapper mapper = new RouteDataMapper(Collections.singleton("MET"), true);

        Assertions.assertTrue(mapper.shouldInclude(ParserBuilder.getRecordFor(routeA)));
        Assertions.assertFalse(mapper.shouldInclude(ParserBuilder.getRecordFor(routeB)));
    }

    @Test
    void shouldParseRoute() throws IOException {
        RouteDataMapper routeDataParser = new RouteDataMapper(Collections.emptySet(), true);
        routeDataParser.initColumnIndex(ParserBuilder.getRecordFor("route_id,agency_id,route_short_name,route_long_name,route_type"));

        RouteData result = routeDataParser.parseEntry(ParserBuilder.getRecordFor(routeA));

        assertThat(result.getId()).isEqualTo(IdFor.createId("MET:MET4:O:"));
        assertThat(result.getShortName()).isEqualTo("MET4");
        assertThat(result.getLongName()).isEqualTo("Ashton-Under-Lyne - Manchester - Eccles");
        assertThat(result.getAgencyId()).isEqualTo(IdFor.createId("MET"));
    }

    @Test
    void shouldParseBusRoute() throws IOException {
        RouteDataMapper routeDataParser = new RouteDataMapper(Collections.emptySet(), true);
        routeDataParser.initColumnIndex(ParserBuilder.getRecordFor("route_id,agency_id,route_short_name,route_long_name,route_type"));

        RouteData result = routeDataParser.parseEntry(ParserBuilder.getRecordFor(problemBusRoute));

        assertThat(result.getId()).isEqualTo(IdFor.createId("JSC: 588:C:"));
        assertThat(result.getShortName().trim()).isEqualTo("588");
        assertThat(result.getLongName()).isEqualTo("Leigh - Lowton, Scott Road");
        assertThat(result.getAgencyId()).isEqualTo(IdFor.createId("JSC"));
    }

}