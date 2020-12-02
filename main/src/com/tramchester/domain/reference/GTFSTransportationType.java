package com.tramchester.domain.reference;

import com.tramchester.domain.EnumParser;

import java.util.*;

public enum GTFSTransportationType {

    tram("0"),
    subway("1"),
    train("2"),
    bus("3"),
    ferry("4"),
    cableTram("5"),
    aerialLift("6"),
    funicular("7"),
    trolleyBus("11"),
    monorail("12");

    public final static EnumParser<GTFSTransportationType> parser;
    private static final Map<String, GTFSTransportationType> textMap;

    static {
        textMap = new HashMap<>();
        GTFSTransportationType[] valid = GTFSTransportationType.values();
        for (GTFSTransportationType value : valid) {
            textMap.put(value.getText(), value);
        }
        parser = new EnumParser<>(textMap);
    }

    private final String text;

    GTFSTransportationType(String theText) {
        this.text = theText;
    }

    private String getText() {
        return text;
    }
}
