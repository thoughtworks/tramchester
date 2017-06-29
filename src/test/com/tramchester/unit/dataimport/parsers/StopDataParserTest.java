package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.parsers.StopDataParser;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StopDataParserTest {

    private StopDataParser stopDataParser;

    @Before
    public void beforeEachTestRuns() {
        stopDataParser = new StopDataParser();
    }

    @Test
    public void shouldParseTramStop() throws Exception {
        String[] stop = new String[] {"9400ZZMAWYT2", "mantwjdt",
                "\"Wythenshawe,Wythenshawe Town Centre (Manchester Metrolink)\""
                , "53.38003", "-2.26381",
                "http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=9400ZZMAWYT2"};

        StopData stopData = stopDataParser.parseEntry(stop);

        assertThat(stopData.getId()).isEqualTo("9400ZZMAWYT2");
        assertThat(stopData.getCode()).isEqualTo("mantwjdt");
        assertThat(stopData.getArea()).isEqualTo("Wythenshawe");
        assertThat(stopData.getName()).isEqualTo("Wythenshawe Town Centre");
        assertThat(stopData.getLatitude()).isEqualTo(53.38003);
        assertThat(stopData.getLongitude()).isEqualTo(-2.26381);
        assertThat(stopData.isTram()).isEqualTo(true);
    }

    @Test
    public void shouldParseTramStopMultipleCommas() throws Exception {
        String[] stop = new String[] {"800NEH0341","missing","\"Alkrington Garden Village, nr School Evesham Road (E bnd, Hail and ride)\"",
                "53.53509","-2.19333",
                "http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=1800NEH0341" };
        StopData stopData = stopDataParser.parseEntry(stop);

        assertThat(stopData.getId()).isEqualTo("800NEH0341");
        assertThat(stopData.getCode()).isEqualTo("missing");
        assertThat(stopData.getArea()).isEqualTo("Alkrington Garden Village");
        assertThat(stopData.getName()).isEqualTo("nr School Evesham Road (E bnd, Hail and ride)");
        assertThat(stopData.getLatitude()).isEqualTo(53.53509);
        assertThat(stopData.getLongitude()).isEqualTo(-2.19333);
        assertThat(stopData.isTram()).isEqualTo(false);
    }
}