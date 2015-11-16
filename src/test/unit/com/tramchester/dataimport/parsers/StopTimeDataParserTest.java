package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StopTimeDataParserTest {
    private String stop = "Trip000001,06:41:10,06:42:30,9400ZZMAABM1,0001,0,1\n";

    @Test
    public void shouldParseStop() throws Exception {
        StopTimeDataParser stopTimeDataParser = new StopTimeDataParser();
        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(this.stop.split(","));

        assertThat(stopTimeData.getTripId()).isEqualTo("Trip000001");
        assertThat(stopTimeData.getArrivalTime().toString()).isEqualTo("06:41:10");
        assertThat(stopTimeData.getDepartureTime().toString()).isEqualTo("06:42:30");
        assertThat(stopTimeData.getStopId().toString()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTimeData.getStopSequence()).isEqualTo("0001");
    }
}