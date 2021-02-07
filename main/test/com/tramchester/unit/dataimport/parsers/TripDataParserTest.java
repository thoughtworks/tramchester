package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.id.StringIdFor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripDataParserTest extends ParserTestHelper<TripData> {

    @BeforeEach
    void beforeEach() {
        super.before(TripData.class,"route_id,service_id,trip_id,trip_headsign");
    }

    @Test
    void shouldParseMetrolink() {

        TripData tripData = parse("MET:MET1:I:,Serv000001,Trip000001,\"Bury,Bury Interchange (Manchester Metrolink)\"");

        assertThat(tripData.getRouteId()).isEqualTo(StringIdFor.createId("MET:MET1:I:"));
        assertThat(tripData.getServiceId()).isEqualTo(StringIdFor.createId("Serv000001"));
        assertThat(tripData.getTripId()).isEqualTo(StringIdFor.createId("Trip000001"));
        assertThat(tripData.getHeadsign()).isEqualTo("Bury Interchange");
    }

    @Test
    void shouldParseOther() {
        TripData tripData = parse("CBL: 157:I:,Serv000153,Trip004334,\"Garswood, Garswood Station\"");

        assertThat(tripData.getRouteId()).isEqualTo(StringIdFor.createId("CBL:157:I:"));
        assertThat(tripData.getServiceId()).isEqualTo(StringIdFor.createId("Serv000153"));
        assertThat(tripData.getTripId()).isEqualTo(StringIdFor.createId("Trip004334"));
        assertThat(tripData.getHeadsign()).isEqualTo("Garswood, Garswood Station");
    }

}