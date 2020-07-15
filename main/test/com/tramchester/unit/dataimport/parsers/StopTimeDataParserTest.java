package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.parsers.StopTimeDataMapper;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StopTimeDataParserTest {
    private StopTimeDataMapper stopTimeDataParser;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        stopTimeDataParser = new StopTimeDataMapper(Collections.emptySet());
        stopTimeDataParser.initColumnIndex(ParserBuilder.getRecordFor("trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type"));

    }

    @Test
    void shouldFilter() throws IOException {
        StopTimeDataMapper filteringMapper = new StopTimeDataMapper(Collections.singleton("Trip000001"));
        filteringMapper.initColumnIndex(ParserBuilder.getRecordFor("trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type"));
        String stopA = "Trip000001,06:41:00,06:42:00,9400ZZMAABM1,0001,0,1\n";
        String stopB = "Trip000002,06:41:00,06:42:00,9400ZZMAABM1,0001,0,1\n";

        Assertions.assertTrue(filteringMapper.shouldInclude(ParserBuilder.getRecordFor(stopA)));
        Assertions.assertFalse(filteringMapper.shouldInclude(ParserBuilder.getRecordFor(stopB)));
    }

    @Test
    void shouldParseStop() throws TramchesterException, IOException {
        String stop = "Trip000001,06:41:00,06:42:00,9400ZZMAABM1,0001,0,1\n";

        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(ParserBuilder.getRecordFor(stop));

        assertThat(stopTimeData.getTripId()).isEqualTo("Trip000001");
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(6,41));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(6,42));
        assertThat(stopTimeData.getStopId()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTimeData.getStopSequence()).isEqualTo("0001");
    }

    @Test
    void shouldCopeWith24TimeFormatInData() throws TramchesterException, IOException {
        String stop = "Trip000001,24:00:00,24:00:00,9400ZZMAABM1,0001,0,1\n";

        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(ParserBuilder.getRecordFor(stop));

        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(0,0));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(0,0));
    }

    @Test
    void shouldCopeWith25TimeFormatInData() throws TramchesterException, IOException {
        String stop = "Trip000001,25:05:00,25:07:00,9400ZZMAABM1,0001,0,1\n";

        StopTimeData stopTimeData = stopTimeDataParser.parseEntry(ParserBuilder.getRecordFor(stop));

        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(1,5));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(1,7));
    }

}