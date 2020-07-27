package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.parsers.StopDataMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

class StopDataParserTest {

    private final String tramStop = "9400ZZMAWYT2,mantwjdt,\"Wythenshawe, Wythenshawe Town Centre (Manchester Metrolink)\",53.38003," +
            "-2.26381,http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=9400ZZMAWYT2";


    private final String tfgmBusStop = "800NEH0341,missing,\"Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)\",53.53509,-2.19333"+
            ",http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=1800NEH0341";

    @Test
    void shouldFilterCorrectly() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.singleton("9400ZZMAWYT2"));

        stopDataParser.initColumnIndex(ParserBuilder.getRecordFor("stop_id,stop_code,stop_name,stop_lat,stop_lon"));

        Assertions.assertTrue(stopDataParser.shouldInclude(ParserBuilder.getRecordFor(tramStop)));
        Assertions.assertFalse(stopDataParser.shouldInclude(ParserBuilder.getRecordFor(tfgmBusStop)));
    }

    @Test
    void shouldParseTramStop() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.emptySet());

        stopDataParser.initColumnIndex(ParserBuilder.getRecordFor("stop_id,stop_code,stop_name,stop_lat,stop_lon"));
        StopData stopData = stopDataParser.parseEntry(ParserBuilder.getRecordFor(tramStop));

        assertThat(stopData.getId()).isEqualTo("9400ZZMAWYT2");
        assertThat(stopData.getCode()).isEqualTo("mantwjdt");
        assertThat(stopData.getArea()).isEqualTo("Wythenshawe");
        assertThat(stopData.getName()).isEqualTo("Wythenshawe Town Centre");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.38003);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-2.26381);
        assertThat(stopData.isTFGMTram()).isEqualTo(true);
        assertThat(stopData.getGridPosition().getEastings()).isEqualTo(382548);
        assertThat(stopData.getGridPosition().getNorthings()).isEqualTo(387052);

    }

    @Test
    void shouldParseTFGMBusStop() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.emptySet());

        stopDataParser.initColumnIndex(ParserBuilder.getRecordFor("stop_id,stop_code,stop_name,stop_lat,stop_lon"));

        StopData stopData = stopDataParser.parseEntry(ParserBuilder.getRecordFor(tfgmBusStop));

        assertThat(stopData.getId()).isEqualTo("800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("missing");
        assertThat(stopData.getArea()).isEqualTo("Alkrington Garden Village");
        assertThat(stopData.getName()).isEqualTo("Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.53509);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-2.19333);
        assertThat(stopData.isTFGMTram()).isEqualTo(false);
        assertThat(stopData.getGridPosition().getEastings()).isEqualTo(387284);
        assertThat(stopData.getGridPosition().getNorthings()).isEqualTo(404288);
    }

    @Test
    void shouldParseTrainStop() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.emptySet());

        stopDataParser.initColumnIndex(ParserBuilder.getRecordFor(
                "stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type," +
                        "parent_station,stop_timezone,wheelchair_boarding"));

        String trainStop = "HOP,HOPD,Hope (Derbyshire),0,53.34611,-1.72989,,,,,Europe/London,0";

        StopData stopData = stopDataParser.parseEntry(ParserBuilder.getRecordFor(trainStop));

        assertThat(stopData.getId()).isEqualTo("HOP");
        assertThat(stopData.getCode()).isEqualTo("HOPD");
        assertThat(stopData.getArea()).isEqualTo("");
        assertThat(stopData.getName()).isEqualTo("Hope (Derbyshire)");
        assertThat(stopData.getLatLong().getLat()).isEqualTo(53.34611);
        assertThat(stopData.getLatLong().getLon()).isEqualTo(-1.72989);
        assertThat(stopData.isTFGMTram()).isEqualTo(false);
        assertThat(stopData.getGridPosition().getEastings()).isEqualTo(418080);
        assertThat(stopData.getGridPosition().getNorthings()).isEqualTo(383280);

    }
}