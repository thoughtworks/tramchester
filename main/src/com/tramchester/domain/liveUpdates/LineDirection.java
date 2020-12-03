package com.tramchester.domain.liveUpdates;

public enum LineDirection {
    Incoming, Outgoing, Both, Unknown;

    public static LineDirection Reverse(LineDirection lineDirection) {
        switch (lineDirection) {
            case Incoming: return Outgoing;
            case Outgoing: return Incoming;
            default:
                return lineDirection;
        }
    }
}
