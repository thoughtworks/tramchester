package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.*;
import com.tramchester.repository.WriteableTransportData;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class RailTimetableMapper {
    private State currentState;
    private CurrentSchedule currentSchedule;
    private final CreatesTransportData processor;

    public RailTimetableMapper(WriteableTransportData container) {
        currentState = State.Between;
        processor = new CreatesTransportData(container);
    }

    public void seen(RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case BasicSchedule -> seenBegin(record);
            case TerminatingLocation -> seenEnd(record);
            case OriginLocation -> seenOrigin(record);
            case IntermediateLocation -> seenIntermediate(record);
        }
    }

    private void seenIntermediate(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        currentSchedule.addIntermediate(record);
    }

    private void seenOrigin(RailTimetableRecord record) {
        guardState(State.SeenSchedule, record);
        currentSchedule.addOrigin(record);
        currentState = State.SeenOrigin;
    }

    private void seenEnd(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        currentSchedule.finish(record);
        processor.consume(currentSchedule);
        currentState = State.Between;
    }

    private void seenBegin(RailTimetableRecord record) {
        BasicSchedule basicSchedule = (BasicSchedule) record;
        guardState(State.Between, record);

        currentSchedule = new CurrentSchedule(basicSchedule);
        if (basicSchedule.getSTPIndicator()== BasicSchedule.ShortTermPlanIndicator.Cancellation) {
            processor.cancelSchedule(basicSchedule);
            currentState = State.Between;
        } else {
            currentState = State.SeenSchedule;
        }
    }

    private enum State {
        SeenSchedule,
        SeenOrigin,
        Between
    }

    private void guardState(State expectedState, RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    private static class CurrentSchedule {

        private final BasicSchedule basicScheduleRecord;
        private final List<IntermediateLocation> intermediateLocations;
        private OriginLocation originLocation;
        private TerminatingLocation terminatingLocation;

        public CurrentSchedule(RailTimetableRecord basicScheduleRecord) {
            this.basicScheduleRecord = (BasicSchedule) basicScheduleRecord;
            intermediateLocations = new ArrayList<>();
        }

        public void addIntermediate(RailTimetableRecord record) {
            intermediateLocations.add((IntermediateLocation) record);
        }

        public void addOrigin(RailTimetableRecord record) {
            this.originLocation = (OriginLocation) record;
        }

        public void finish(RailTimetableRecord record) {
            this.terminatingLocation = (TerminatingLocation) record;
        }
    }

    private static class CreatesTransportData {
        private final WriteableTransportData container;

        private CreatesTransportData(WriteableTransportData container) {
            this.container = container;
        }

        public void consume(CurrentSchedule currentSchedule) {

        }

        public void cancelSchedule(BasicSchedule basicSchedule) {

        }
    }
}
