package com.tramchester.domain;

public enum TransportMode {
    Bus, Tram, Depart, Board, Walk;

   static boolean isVehicle(TransportMode item) {
       return (item==Tram) || (item==Bus);
   }

    static boolean isWalk(TransportMode item) {
        return (item==Walk);
    }

   public boolean isVehicle() {
       return isVehicle(this);
   }

    public boolean isWalk() {
        return isWalk(this);
    }
}
