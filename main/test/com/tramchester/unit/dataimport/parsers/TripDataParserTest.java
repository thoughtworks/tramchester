package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.dataimport.parsers.TripDataMapper;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

public class TripDataParserTest {
    private String tripA = "MET:MET1:I:,Serv000001,Trip000001,\"Bury,Bury Interchange (Manchester Metrolink)\"";
    private String tripB = "MET:MET1:X:,Serv000001,Trip000001,\"Bury,Bury Interchange (Manchester Metrolink)\"";

    @Test
    public void shouldFilter() throws IOException {
        TripDataMapper tripParserTest = new TripDataMapper(Collections.singleton("MET:MET1:I:"));

        assertTrue(tripParserTest.shouldInclude(ParserBuilder.getRecordFor(tripA)));
        assertFalse(tripParserTest.shouldInclude(ParserBuilder.getRecordFor(tripB)));
    }

    @Test
    public void shouldParseTrip() throws IOException {
        TripDataMapper tripParserTest = new TripDataMapper(Collections.emptySet());

        CSVRecord record = ParserBuilder.getRecordFor(tripA);

        TripData tripData = tripParserTest.parseEntry(record);

        assertThat(tripData.getRouteId()).isEqualTo("MET:MET1:I:");
        assertThat(tripData.getServiceId()).isEqualTo("Serv000001");
        assertThat(tripData.getTripId()).isEqualTo("Trip000001");
        assertThat(tripData.getTripHeadsign()).isEqualTo("Bury Interchange");
    }

}