package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.time.TramTime;
import com.tramchester.unit.dataimport.ParserTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StopTimeDataParseTest extends ParserTestHelper<StopTimeData> {

    @BeforeEach
    void beforeEachTestRuns() {
        String header = "trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type";
        super.before(StopTimeData.class, header);
    }

    @Test
    void shouldParseStop() {
        String stop = "Trip000001,06:41:00,06:42:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getTripId()).isEqualTo(StringIdFor.createId("Trip000001"));
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(6, 41));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(6, 42));
        assertThat(stopTimeData.getStopId()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTimeData.getStopSequence()).isEqualTo(1);
    }

    @Test
    void shouldParseStopSingleDigitHour() {
        String stop = "Trip000001,6:41:00,6:42:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getTripId()).isEqualTo(StringIdFor.createId("Trip000001"));
        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.of(6, 41));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.of(6, 42));
        assertThat(stopTimeData.getStopId()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTimeData.getStopSequence()).isEqualTo(1);
    }

    @Test
    void shouldCopeWith24TimeFormatInData() {
        String stop = "Trip000001,24:00:00,24:00:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.nextDay(0,0));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.nextDay(0,0));
    }

    @Test
    void shouldCopeWith25TimeFormatInData() {
        String stop = "Trip000001,25:05:00,25:07:00,9400ZZMAABM1,0001,0,1";

        StopTimeData stopTimeData = parse(stop);

        assertThat(stopTimeData.getArrivalTime()).isEqualTo(TramTime.nextDay(1,5));
        assertThat(stopTimeData.getDepartureTime()).isEqualTo(TramTime.nextDay(1,7));

    }

}