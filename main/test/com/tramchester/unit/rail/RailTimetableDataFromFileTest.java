package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.RailTimetableDataFromFile;
import com.tramchester.dataimport.rail.RailDataRecordFactory;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.TIPLOCInsert;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RailTimetableDataFromFileTest extends EasyMockSupport {

    private RailTimetableDataFromFile railTimetableDataFromFile;
    private RailDataRecordFactory factory;

    @BeforeEach
    void beforeEachTestRuns() {
        factory = createMock(RailDataRecordFactory.class);
        railTimetableDataFromFile = new RailTimetableDataFromFile(Path.of("somePath"), factory);
    }

    @Test
    void shouldConsumeSomething() {
        final String line = "TIAACHEN 00081601LAACHEN                    00005   0";
        StringReader reader = new StringReader(line + System.lineSeparator());

        final TIPLOCInsert tiplocInsert = new TIPLOCInsert(line);
        EasyMock.expect(factory.createTIPLOC(line)).andReturn(tiplocInsert);

        replayAll();
        Stream<RailTimetableRecord> results = railTimetableDataFromFile.load(reader);
        assertEquals(1, results.filter(item -> item.equals(tiplocInsert)).count());
        verifyAll();
    }
}
