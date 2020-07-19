package com.tramchester.domain;

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

    @Override
    public TransportMode getTransportMode() {
        return this;
    }
}
