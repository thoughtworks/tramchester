package com.tramchester.domain.liveUpdates;

public class DueTram {
    private final String status;
    private String destination;
    private int wait;
    private String carriages;

    public DueTram(String destination, String status, int wait, String carriages) {

        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
    }

    public String getDestination() {
        return destination;
    }

    public String getStatus() {
        return status;
    }

    public int getWait() {
        return wait;
    }

    public String getCarriages() {
        return carriages;
    }
}
