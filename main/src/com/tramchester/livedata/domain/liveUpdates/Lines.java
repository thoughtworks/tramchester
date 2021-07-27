package com.tramchester.livedata.domain.liveUpdates;

// Live data lines are not same as timetable routes, see also Mapper
public enum Lines {
    Altrincham("Altrincham"),
    Airport("Airport"),
    Bury("Bury"),
    Eccles("Eccles"),
    EastManchester("East Manchester"),
    OldhamAndRochdale("Oldham & Rochdale"),
    SouthManchester("South Manchester"),
    TraffordPark("Trafford Park"),
    UnknownLine("Unknown");

    private final String name;

    Lines(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
