package com.tramchester.unit.rail;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.rail.LoadRailTimetableRecords;
import com.tramchester.dataimport.rail.RailDataRecordFactory;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RailLoadTimetableFromFileTest extends EasyMockSupport {

    private LoadRailTimetableRecords loadRailTimetableRecords;
    private RailDataRecordFactory factory;
    private final TramchesterConfig config = TestEnv.GET();

    @BeforeEach
    void beforeEachTestRuns() {
        factory = createMock(RailDataRecordFactory.class);
        UnzipFetchedData.Ready ready = UnzipFetchedData.Ready.fakeForTestingOnly();
        loadRailTimetableRecords = new LoadRailTimetableRecords(config, factory, ready);
    }

    @Test
    void shouldConsumeLinesAndCreateCorrectRecords() {
        final String lineA = "TIAACHEN 00081601LAACHEN                    00005   0";
        final String lineB = "BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P";
        final String lineC = "LINEWSGAT 1852H1853      18531854123      T";
        final String lineD = "LOLINCLNC 1237 12384A        TB";
        final String lineE = "LTWLWYNGC 1918 19184     TF";

        final String sep = System.lineSeparator();
        StringReader reader = new StringReader(lineA + sep + lineB + sep + lineC + sep + lineD + sep + lineE + sep);

        EasyMock.expect(factory.createTIPLOC(lineA)).andReturn(createMock(TIPLOCInsert.class));
        EasyMock.expect(factory.createBasicSchedule(lineB)).andReturn(createMock(BasicSchedule.class));
        EasyMock.expect(factory.createIntermediate(lineC)).andReturn(createMock(IntermediateLocation.class));
        EasyMock.expect(factory.createOrigin(lineD)).andReturn(createMock(OriginLocation.class));
        EasyMock.expect(factory.createTerminating(lineE)).andReturn(createMock(TerminatingLocation.class));

        replayAll();
        Stream<RailTimetableRecord> stream = loadRailTimetableRecords.load(reader);
        List<RailTimetableRecord> results = stream.collect(Collectors.toList());
        verifyAll();

        assertEquals(5, results.size());
        assertTrue(TIPLOCInsert.class.isAssignableFrom(results.get(0).getClass()));
        assertTrue(BasicSchedule.class.isAssignableFrom(results.get(1).getClass()));
        assertTrue(IntermediateLocation.class.isAssignableFrom(results.get(2).getClass()));
        assertTrue(OriginLocation.class.isAssignableFrom(results.get(3).getClass()));
        assertTrue(TerminatingLocation.class.isAssignableFrom(results.get(4).getClass()));

    }

}
