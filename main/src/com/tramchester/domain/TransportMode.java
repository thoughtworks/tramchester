package com.tramchester.domain;

public enum TransportMode {
    Bus, Tram, Depart, Board, Walk, Connect, Train;

    public static TransportMode fromGTFS(GTFSTransportationType routeType) {
        switch (routeType) {
            case tram: return TransportMode.Tram;
            case bus: return TransportMode.Bus;
            case train: return TransportMode.Train;
            default:
                throw new RuntimeException("Unexpected route type " + routeType);
        }
    }
}
