package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.RailTimetableDataFromFile;
import com.tramchester.dataimport.rail.RailDataRecordFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Path;

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
        factory.createTIPLOC(line);
        EasyMock.expectLastCall();
        StringReader reader = new StringReader(line +System.lineSeparator());

        replayAll();
        railTimetableDataFromFile.load(reader);
        verifyAll();
    }
}
