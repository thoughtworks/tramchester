package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
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
    public void shouldParseBusStopWithStreetAndTownUnquoted()
    {
        String[] stop = new String[] {"1800EB05551","mantdwgj",
                "Rusholme"," Anson Road/St. Anselm Hall (Stop B)"
                ,"53.45412","-2.21209",
                "http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=1800EB05551"};

        StopData stopData = stopDataParser.parseEntry(stop);

        assertThat(stopData.getId()).isEqualTo("1800EB05551");
        assertThat(stopData.getCode()).isEqualTo("mantdwgj");
        assertThat(stopData.getName()).isEqualTo("Anson Road/St. Anselm Hall (Stop B)");
        assertThat(stopData.getArea()).isEqualTo("Rusholme");
        assertThat(stopData.getLatitude()).isEqualTo(53.45412);
        assertThat(stopData.getLongitude()).isEqualTo(-2.21209);
        assertThat(stopData.isTram()).isEqualTo(false);

    }

}