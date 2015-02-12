package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StopDataParserTest {
    private String stop = "9400ZZMAWYT2, mantwjdt, \"Wythenshawe,Wythenshawe Town Centre (Manchester Metrolink)\", 53.38003, -2.26381, http://www.transportdirect.info/web2/journeyplanning/StopInformationLandingPage.aspx?et=si&id=GTDF&ef=m&st=n&sd=9400ZZMAWYT2";

    @Test
    public void shouldParseStop() throws Exception {
        StopDataParser stopDataParser = new StopDataParser();
        StopData stopData = stopDataParser.parseEntry(this.stop.split(", "));

        assertThat(stopData.getId()).isEqualTo("9400ZZMAWYT");
        assertThat(stopData.getCode()).isEqualTo("mantwjdt");
        assertThat(stopData.getName()).isEqualTo("Wythenshawe Town Centre");
    }
}