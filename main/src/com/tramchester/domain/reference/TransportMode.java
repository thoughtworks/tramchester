package com.tramchester.domain.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;
import org.apache.commons.collections4.SetUtils;

import java.util.*;

public enum TransportMode implements HasTransportMode {
    Bus((short)1),
    Tram((short)2),
    Train((short)3),
    Walk((short)4),
    Ferry((short)5),
    Subway((short)6),
    RailReplacementBus((short)7),
    Ship((short)8),

    Connect((short)52),

    NotSet((short)53),

    Unknown((short)999);

    private static final Map<Short, TransportMode> index;

    static {
        index = new HashMap<>();
        for(TransportMode mode : EnumSet.allOf(TransportMode.class)) {
            index.put(mode.graphId, mode);
        }
    }

    @JsonIgnore
    private final short graphId;

    TransportMode(short graphId) {
        this.graphId = graphId;
    }

    public static boolean isTram(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Tram);
    }

    public static boolean isTram(HasTransportModes hasModes) {
        return hasModes.getTransportModes().contains(TransportMode.Tram);
    }

    public static TransportMode fromNumber(short number) {
        return index.get(number);
    }

    public static Set<TransportMode> fromNumbers(short[] numbers) {
        Set<TransportMode> result = new HashSet<>();
        for (short value : numbers) {
            result.add(index.get(value));
        }
        return result;
    }

    public static boolean intersects(Set<TransportMode> modesA, Set<TransportMode> modesB) {
        return !SetUtils.intersection(modesA, modesB).isEmpty();
    }

    @JsonIgnore
    @Override
    public TransportMode getTransportMode() {
        return this;
    }

    public short getNumber() {
        return graphId;
    }
}
