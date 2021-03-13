package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteDataParserTest extends ParserTestHelper<RouteData> {

    @BeforeEach
    void beforeEach() {
        super.before(RouteData.class, "route_id,agency_id,route_short_name,route_long_name,route_type");
    }

    @Test
    void shouldParseTramRouteOldFormat() {
        RouteData result = parse("MET:MET4:O:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");

        assertThat(result.getId()).isEqualTo(StringIdFor.createId("MET:MET4:O:"));
        assertThat(result.getShortName()).isEqualTo("MET4");
        assertThat(result.getLongName()).isEqualTo("Ashton-Under-Lyne - Manchester - Eccles");
        assertThat(result.getAgencyId()).isEqualTo(StringIdFor.createId("MET"));
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);
    }

    @Test
    void shouldParseTramRouteInboundOldFormat() {
        RouteData result = parse("MET:MET4:I:,MET,MET4,Ashton-Under-Lyne - Manchester - Eccles,0");

        assertThat(result.getId()).isEqualTo(StringIdFor.createId("MET:MET4:I:"));
        assertThat(result.getShortName()).isEqualTo("MET4");
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);
    }

    @Test
    void shouldParseTramRoute() {
        RouteData result = parse("METLBLUE:O:2021-03-08,METL,Blue Line,Eccles - Manchester - Ashton Under Lyne,0");

        assertThat(result.getId()).isEqualTo(StringIdFor.createId("METLBLUE:O:2021-03-08"));
        assertThat(result.getShortName()).isEqualTo("Blue Line");
        assertThat(result.getLongName()).isEqualTo("Eccles - Manchester - Ashton Under Lyne");
        assertThat(result.getAgencyId()).isEqualTo(TestEnv.MetAgency().getId());
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);

    }

    @Test
    void shouldParseTramRouteInbound() {
        RouteData result = parse("METLBLUE:I:2021-03-08,METL,Blue Line,Ashton Under Lyne - Manchester - Eccles,0");

        assertThat(result.getId()).isEqualTo(StringIdFor.createId("METLBLUE:I:2021-03-08"));
        assertThat(result.getShortName()).isEqualTo("Blue Line");
        assertThat(result.getLongName()).isEqualTo("Ashton Under Lyne - Manchester - Eccles");
        assertThat(result.getAgencyId()).isEqualTo(TestEnv.MetAgency().getId());
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.tram);
    }

    @Test
    void shouldParseBusRoute() {
        RouteData result = parse("JSC: 588:C:,JSC, 588,\"Leigh - Lowton, Scott Road\",3");

        assertThat(result.getId()).isEqualTo(StringIdFor.createId("JSC:588:C:"));
        assertThat(result.getShortName().trim()).isEqualTo("588");
        assertThat(result.getLongName()).isEqualTo("Leigh - Lowton, Scott Road");
        assertThat(result.getAgencyId()).isEqualTo(StringIdFor.createId("JSC"));
        assertThat(result.getRouteType()).isEqualTo(GTFSTransportationType.bus);
    }

}