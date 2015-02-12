package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StopTimeDataParserTest {
    private String stop = "Trip000001,06:41:00,06:41:00,9400ZZMAABM1,0001,0,1\n";

    @Test
    public void shouldParseStop() throws Exception {
        StopTimeDataParser stopTimeDataParser = new StopTimeDataParser();
        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(this.stop.split(","));

        assertThat(stopTimeData.getTripId()).isEqualTo("Trip000001");
        assertThat(stopTimeData.getArrivalTime().toString()).isEqualTo("2000-01-01T06:41:00.000Z");
        assertThat(stopTimeData.getDepartureTime().toString()).isEqualTo("2000-01-01T06:41:00.000Z");
        assertThat(stopTimeData.getStopId().toString()).isEqualTo("9400ZZMAABM");
        assertThat(stopTimeData.getStopSequence()).isEqualTo("0001");
    }
}