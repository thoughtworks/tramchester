package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class StopTimeDataParserTest {
    private String stop = "Trip000001,06:41:10,06:42:30,9400ZZMAABM1,0001,0,1\n";
    private String exampleError = "Trip041339,38:58:00,38:58:00,1800STBS001,0045,1,0\n";
    private StopTimeDataParser stopTimeDataParser;

    @Before
    public void beforeEachTestRuns() {
        stopTimeDataParser = new StopTimeDataParser();
    }

    @Test
    public void shouldParseStop() throws Exception {
        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(stop.split(","));

        assertFalse(stopTimeData.isInError());
        assertThat(stopTimeData.getTripId()).isEqualTo("Trip000001");
        assertThat(stopTimeData.getArrivalTime().toString()).isEqualTo("06:41:10");
        assertThat(stopTimeData.getDepartureTime().toString()).isEqualTo("06:42:30");
        assertThat(stopTimeData.getStopId().toString()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTimeData.getStopSequence()).isEqualTo("0001");
    }

    @Test
    public void shouldHandleErrorsInStopParse() {
        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(exampleError.split(","));
        assertTrue(stopTimeData.isInError());
    }
}