package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.parsers.StopDataMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

public class StopDataParserTest {

    private String stopA = "9400ZZMAWYT2,mantwjdt,\"Wythenshawe,Wythenshawe Town Centre (Manchester Metrolink)\",53.38003," +
            "-2.26381,http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=9400ZZMAWYT2";

    private String stopB = "800NEH0341,missing,\"Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)\",53.53509,-2.19333"+
            ",http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=1800NEH0341";


    @Test
    public void shouldFilterCorrectly() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.singleton("9400ZZMAWYT2"));

        assertTrue(stopDataParser.shouldInclude(ParserBuilder.getRecordFor(stopA)));
        assertFalse(stopDataParser.shouldInclude(ParserBuilder.getRecordFor(stopB)));
    }

    @Test
    public void shouldParseTramStop() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.emptySet());

        StopData stopData = stopDataParser.parseEntry(ParserBuilder.getRecordFor(stopA));

        assertThat(stopData.getId()).isEqualTo("9400ZZMAWYT2");
        assertThat(stopData.getCode()).isEqualTo("mantwjdt");
        assertThat(stopData.getArea()).isEqualTo("Wythenshawe");
        assertThat(stopData.getName()).isEqualTo("Wythenshawe Town Centre");
        assertThat(stopData.getLatitude()).isEqualTo(53.38003);
        assertThat(stopData.getLongitude()).isEqualTo(-2.26381);
        assertThat(stopData.isTram()).isEqualTo(true);
    }

    @Test
    public void shouldParseTramStopMultipleCommas() throws IOException {
        StopDataMapper stopDataParser = new StopDataMapper(Collections.emptySet());

        StopData stopData = stopDataParser.parseEntry(ParserBuilder.getRecordFor(stopB));

        assertThat(stopData.getId()).isEqualTo("800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("missing");
        assertThat(stopData.getArea()).isEqualTo("Alkrington Garden Village");
        assertThat(stopData.getName()).isEqualTo("nr School Evesham Road (E bnd, Hail and ride)");
        assertThat(stopData.getLatitude()).isEqualTo(53.53509);
        assertThat(stopData.getLongitude()).isEqualTo(-2.19333);
        assertThat(stopData.isTram()).isEqualTo(false);
    }
}