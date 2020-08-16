package com.tramchester.domain;

import java.util.HashMap;
import java.util.Map;

public enum GTFSPickupDropoffType {
    Regular("0"), // OR empty string
    None("1"),
    Phone("2"),
    Driver("3");

    private static final Map<String, GTFSPickupDropoffType> textMap;
    public final static EnumParser<GTFSPickupDropoffType> parser;

    static {
        textMap = new HashMap<>();
        textMap.put("", Regular);
        GTFSPickupDropoffType[] valid = GTFSPickupDropoffType.values();
        for (GTFSPickupDropoffType value : valid) {
            textMap.put(value.getText(), value);
        }
        parser =  new EnumParser<>(textMap);
    }

    private final String text;

    GTFSPickupDropoffType(String text) {
        this.text = text;
    }

    private String getText() {
        return text;
    }

}
