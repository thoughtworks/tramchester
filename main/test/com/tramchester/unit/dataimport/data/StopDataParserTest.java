package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.unit.dataimport.ParserTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopDataParserTest extends ParserTestHelper<StopData> {


    @BeforeEach
    void beforeEach() {
        super.before(StopData.class, "stop_id,stop_code,stop_name,stop_lat,stop_lon,stop_url");
    }

    @Test
    void shouldParseTramStop() {

        String tramStop = "9400ZZMAWYT1,mantwjdw,Wythenshawe Town Centre (Manchester Metrolink),53.38001047220,-2.26370992844";
        StopData stopData = parse(tramStop);

        assertThat(stopData.getId()).isEqualTo("9400ZZMAWYT1");
        assertThat(stopData.getCode()).isEqualTo("mantwjdw");
        assertThat(stopData.getName()).isEqualTo("Wythenshawe Town Centre (Manchester Metrolink)");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.38001047220);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-2.26370992844);
    }

    @Test
    void shouldParseTFGMBusStop() {
        String tfgmBusStop = "1800NEH0341,,Evesham Road,53.53488245121,-2.18746922864";
        StopData stopData = parse(tfgmBusStop);

        assertThat(stopData.getId()).isEqualTo("1800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("");
        assertThat(stopData.getName()).isEqualTo("Evesham Road");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.53488245121);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-2.18746922864);
        assertTrue(stopData.getLatLong().isValid());
    }

    @Test
    void shouldParseTFGMBusStopInvalidPosition() {
        StopData stopData = parse("1800NEH0341,missing,Evesham Road,0.0,0.0");

        assertThat(stopData.getId()).isEqualTo("1800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("missing");
        assertThat(stopData.getName()).isEqualTo("Evesham Road");
        assertFalse(stopData.getLatLong().isValid());
    }

    @Test
    void shouldParseTrainStop() {
        String trainDataHeader = "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type," +
                "parent_station,stop_timezone,wheelchair_boarding";

        super.before(StopData.class, trainDataHeader);

        String trainStop = "HOP,HOPD,Hope (Derbyshire),0,53.34611,-1.72989,,,,,Europe/London,0";

        StopData stopData = parse(trainStop);

        assertThat(stopData.getId()).isEqualTo("HOP");
        assertThat(stopData.getCode()).isEqualTo("HOPD");
        assertThat(stopData.getName()).isEqualTo("Hope (Derbyshire)");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.34611);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-1.72989);
    }
}