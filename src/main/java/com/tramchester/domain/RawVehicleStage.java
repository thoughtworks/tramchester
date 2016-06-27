package com.tramchester.domain;

public class RawVehicleStage implements TransportStage {
    protected final Location firstStation;
    protected final TransportMode mode;
    protected final String routeName;
    protected final String displayClass;
    protected final int startTime;

    protected String serviceId;
    protected Location lastStation;
    private int endTime;

    public RawVehicleStage(Location firstStation, String routeName, TransportMode mode, String displayClass, int startTime) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
        this.startTime = startTime;
    }

    public RawVehicleStage(RawVehicleStage other) {
        this.firstStation = other.firstStation;
        this.routeName = other.routeName;
        this.mode = other.mode;
        this.displayClass = other.displayClass;
        this.startTime = other.startTime;
        this.serviceId = other.serviceId;
        this.lastStation = other.lastStation;
    }

    public String getServiceId() {
        return serviceId;
    }

    public RawVehicleStage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public RawVehicleStage setLastStation(Location lastStation) {
        this.lastStation = lastStation;
        return this;
    }

    public Location getFirstStation() {
        return firstStation;
    }

    public Location getLastStation() {
        return lastStation;
    }

    public Location getActionStation() { return firstStation; }

    public String getRouteName() {
        return routeName;
    }

    @Override
    public TransportMode getMode() {
        return mode;
    }

    @Override
    public boolean getIsAVehicle() {
        return (mode.equals(TransportMode.Bus)) || (mode.equals(TransportMode.Tram));
    }

    public String getDisplayClass() {
        return displayClass;
    }

    @Override
    public String toString() {
        return "RawVehicleStage{" +
                "firstStation='" + firstStation + '\'' +
                ", mode='" + mode + '\'' +
                ", routeName='" + routeName + '\'' +
                ", displayClass='" + displayClass + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", lastStation='" + lastStation + '\'' +
                '}';
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() { return endTime; }

    public void setDepartTime(int endTime) {
        this.endTime = endTime;

    }
}
