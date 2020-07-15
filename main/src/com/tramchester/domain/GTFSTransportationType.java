package com.tramchester.domain;

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

    private static final Map<String, GTFSTransportationType> transportationTypes;
    private static final List<GTFSTransportationType> supported;

    static {
        transportationTypes = new HashMap<>();
        GTFSTransportationType[] valid = GTFSTransportationType.values();
        for (GTFSTransportationType GTFSTransportationType : valid) {
            transportationTypes.put(GTFSTransportationType.getText(), GTFSTransportationType);
        }
        supported = new ArrayList<>();
        supported.addAll(Arrays.asList(tram, bus, train));
    }

    public static boolean validType(String routeType) {
        return transportationTypes.containsKey(routeType);
    }

    public static GTFSTransportationType getType(String text) {
        return transportationTypes.get(text);
    }


    private final String text;
    GTFSTransportationType(String text) {
        this.text = text;
    }

    public static boolean supportedType(GTFSTransportationType routeType) {
        return supported.contains(routeType);
    }

    public String getText() {
        return text;
    }
}
