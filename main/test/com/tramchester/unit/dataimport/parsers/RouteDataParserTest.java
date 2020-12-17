package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.reference.RouteDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteDataParserTest extends ParserTestHelper<RouteData> {

    @BeforeEach
    void beforeEach() {
        super.before(RouteData.class, "route_id,agency_id,route_short_name,route_long_name,route_type");
    }

    @Test
    void shouldParseTramRoute() {
        RouteData result = parse("MET:MET4:O:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");

        assertThat(result.getId()).isEqualTo(IdFor.createId("MET:MET4:O:"));
        assertThat(result.getShortName()).isEqualTo("MET4");
        assertThat(result.getLongName()).isEqualTo("Ashton-Under-Lyne - Manchester - Eccles");
        assertThat(result.getAgencyId()).isEqualTo(IdFor.createId("MET"));
        assertThat(result.getRouteDirection()).isEqualTo(RouteDirection.Outbound);
    }

    @Test
    void shouldParseTramRouteInbound() {
        RouteData result = parse("MET:MET4:I:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");

        assertThat(result.getId()).isEqualTo(IdFor.createId("MET:MET4:I:"));
        assertThat(result.getShortName()).isEqualTo("MET4");
        assertThat(result.getRouteDirection()).isEqualTo(RouteDirection.Inbound);
    }

    @Test
    void shouldParseBusRoute() {
        RouteData result = parse("JSC: 588:C:,JSC, 588,\"Leigh - Lowton, Scott Road\",3");

        assertThat(result.getId()).isEqualTo(IdFor.createId("JSC:588:C:"));
        assertThat(result.getShortName().trim()).isEqualTo("588");
        assertThat(result.getLongName()).isEqualTo("Leigh - Lowton, Scott Road");
        assertThat(result.getAgencyId()).isEqualTo(IdFor.createId("JSC"));
        assertThat(result.getRouteDirection()).isEqualTo(RouteDirection.Circular);

    }

}