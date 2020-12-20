package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopDataParserTest extends ParserTestHelper<StopData> {

    private final String tramStop = "9400ZZMAWYT2,mantwjdt,\"Wythenshawe, Wythenshawe Town Centre (Manchester Metrolink)\",53.38003," +
            "-2.26381,http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=9400ZZMAWYT2";


    private final String tfgmBusStop = "800NEH0341,missing,\"Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)\",53.53509,-2.19333"+
            ",http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=1800NEH0341";


    @BeforeEach
    void beforeEach() {
        super.before(StopData.class, "stop_id,stop_code,stop_name,stop_lat,stop_lon,stop_url");
    }

    @Test
    void shouldParseTramStop() {

        StopData stopData = parse(tramStop);

        assertThat(stopData.getId()).isEqualTo("9400ZZMAWYT2");
        assertThat(stopData.getCode()).isEqualTo("mantwjdt");
        assertThat(stopData.getName()).isEqualTo("Wythenshawe Town Centre");
        assertThat(stopData.getArea()).isEqualTo("Wythenshawe");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.38003);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-2.26381);
        assertThat(stopData.isTFGMTram()).isEqualTo(true);
    }

    @Test
    void shouldParseTFGMBusStop() {
        StopData stopData = parse(tfgmBusStop);

        assertThat(stopData.getId()).isEqualTo("800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("missing");
        assertThat(stopData.getArea()).isEqualTo("Alkrington Garden Village");
        assertThat(stopData.getName()).isEqualTo("Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.53509);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-2.19333);
        assertTrue(stopData.getLatLong().isValid());
        assertThat(stopData.isTFGMTram()).isEqualTo(false);
    }

    @Test
    void shouldParseTFGMBusStopInvalidPosition() {
        StopData stopData = parse("800NEH0341,missing,\"Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)\",0.0,0.0"+
                ",http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=1800NEH0341");

        assertThat(stopData.getId()).isEqualTo("800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("missing");
        assertThat(stopData.getArea()).isEqualTo("Alkrington Garden Village");
        assertThat(stopData.getName()).isEqualTo("Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)");
        assertFalse(stopData.getLatLong().isValid());
        assertThat(stopData.isTFGMTram()).isEqualTo(false);
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
        assertThat(stopData.getArea()).isEqualTo("");
        assertThat(stopData.getName()).isEqualTo("Hope (Derbyshire)");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.34611);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-1.72989);
        assertThat(stopData.isTFGMTram()).isEqualTo(false);
    }
}