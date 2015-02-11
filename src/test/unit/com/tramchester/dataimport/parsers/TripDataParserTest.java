package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.TripData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TripDataParserTest {
    private String trip = "MET:MET1:I:, Serv000001, Trip000001, \"Bury,Bury Interchange (Manchester Metrolink)\"";

    @Test
    public void shouldParseTrip() throws Exception {
        TripDataParser tripParserTest = new TripDataParser();
        TripData tripData = tripParserTest.parseEntry(this.trip.split(", "));

        assertThat(tripData.getRouteId()).isEqualTo("MET:MET1:I:");
        assertThat(tripData.getServiceId()).isEqualTo("Serv000001");
        assertThat(tripData.getTripId()).isEqualTo("Trip000001");
        assertThat(tripData.getTripHeadsign()).isEqualTo("Bury Interchange");
    }
}