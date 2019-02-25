package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.parsers.StopTimeDataParser;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class StopTimeDataParserTest {
    private StopTimeDataParser stopTimeDataParser;

    @Before
    public void beforeEachTestRuns() {
        stopTimeDataParser = new StopTimeDataParser();
    }

    @Test
    public void shouldParseStop() throws TramchesterException {
        String stop = "Trip000001,06:41:00,06:42:00,9400ZZMAABM1,0001,0,1\n";

        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(stop.split(","));

        assertFalse(stopTimeData.isInError());
        assertThat(stopTimeData.getTripId()).isEqualTo("Trip000001");
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.create(6,41));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.create(6,42));
        assertThat(stopTimeData.getStopId()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTimeData.getStopSequence()).isEqualTo("0001");
    }

    @Test
    public void shouldCopeWith24TimeFormatInData() throws TramchesterException {
        String stop = "Trip000001,24:00:00,24:00:00,9400ZZMAABM1,0001,0,1\n";
        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(stop.split(","));

        assertFalse(stopTimeData.isInError());
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.create(0,0));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.create(0,0));
    }

    @Test
    public void shouldCopeWith25TimeFormatInData() throws TramchesterException {
        String stop = "Trip000001,25:05:00,25:07:00,9400ZZMAABM1,0001,0,1\n";
        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(stop.split(","));

        assertFalse(stopTimeData.isInError());
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.create(1,5));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.create(1,7));
    }

    @Test
    public void shouldHandleErrorsInStopParse() {
        String exampleError = "Trip041339,38:58:00,38:58:00,1800STBS001,0045,1,0\n";

        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(exampleError.split(","));
        assertTrue(stopTimeData.isInError());
    }
}