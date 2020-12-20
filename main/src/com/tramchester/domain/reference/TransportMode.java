package com.tramchester.domain.reference;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;

import java.util.HashSet;
import java.util.Set;

public enum TransportMode implements HasTransportMode {
    Bus,
    Tram,
    Train,
    Walk,
    Ferry,
    Subway,

    Depart,
    Board,
    Connect,
    NotSet,
    Unknown;

    public static TransportMode fromGTFS(GTFSTransportationType routeType) {
        switch (routeType) {
            case tram: return TransportMode.Tram;
            case bus: return TransportMode.Bus;
            case train: return TransportMode.Train;
            case ferry: return TransportMode.Ferry;
            case subway: return TransportMode.Subway;
            default:
                throw new RuntimeException("Unexpected route type (check config?) " + routeType);
        }

    }

    public static boolean isTram(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Tram);
    }

    public static boolean isTram(HasTransportModes hasModes) {
        return hasModes.getTransportModes().contains(TransportMode.Tram);
    }

    public static boolean isBus(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Bus);
    }

    public static boolean isBus(HasTransportModes item) {
        return item.getTransportModes().contains(TransportMode.Bus);
    }

    public static boolean isTrain(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Train);
    }

    public static boolean isTrain(HasTransportModes item) {
        return item.getTransportModes().contains(TransportMode.Train);
    }

    public static Set<TransportMode> fromGTFS(Set<GTFSTransportationType> gtfsTransportationTypes) {
        Set<TransportMode> result = new HashSet<>();
        gtfsTransportationTypes.forEach(gtfsTransportationType -> result.add(fromGTFS(gtfsTransportationType)));
        return result;
    }

    @Override
    public TransportMode getTransportMode() {
        return this;
    }
}
