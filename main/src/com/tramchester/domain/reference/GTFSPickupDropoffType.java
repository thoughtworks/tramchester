package com.tramchester.domain.reference;

import java.util.HashMap;
import java.util.Map;

public enum GTFSPickupDropoffType {
    Regular("0"), // OR empty string
    None("1"),
    Phone("2"),
    Driver("3");

    private static final Map<String, GTFSPickupDropoffType> textMap;

    static {
        textMap = new HashMap<>();
        textMap.put("", Regular);
        GTFSPickupDropoffType[] valid = GTFSPickupDropoffType.values();
        for (GTFSPickupDropoffType value : valid) {
            textMap.put(value.getText(), value);
        }
    }

    private final String text;

    GTFSPickupDropoffType(String text) {
        this.text = text;
    }

    public static GTFSPickupDropoffType fromString(String pickupType) {
        return textMap.get(pickupType);
    }

    public String getText() {
        return text;
    }

    public boolean isPickup() {
        return this!=None;
    }

    public boolean isDropOff() {
        return this!=None;
    }
}
