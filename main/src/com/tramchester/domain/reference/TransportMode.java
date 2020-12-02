package com.tramchester.domain.reference;

import com.tramchester.domain.HasTransportMode;

import java.util.HashSet;
import java.util.Set;

public enum TransportMode implements HasTransportMode {
    Bus, Tram, Depart, Board, Walk, Connect, Train, NotSet;

    public static TransportMode fromGTFS(GTFSTransportationType routeType) {
        switch (routeType) {
            case tram: return TransportMode.Tram;
            case bus: return TransportMode.Bus;
            case train: return TransportMode.Train;
            default:
                throw new RuntimeException("Unexpected route type " + routeType);
        }

    }

    public static boolean isTram(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Tram);
    }

    public static boolean isBus(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Bus);
    }

    public static boolean isTrain(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Train);
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
