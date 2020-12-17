package com.tramchester.domain.reference;

import java.util.HashMap;
import java.util.Map;

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

    private static final Map<String, GTFSTransportationType> textMap;

    static {
        textMap = new HashMap<>();
        GTFSTransportationType[] valid = GTFSTransportationType.values();
        for (GTFSTransportationType value : valid) {
            textMap.put(value.getText(), value);
        }
    }

    private final String text;

    GTFSTransportationType(String theText) {
        this.text = theText;
    }

    private String getText() {
        return text;
    }
}
