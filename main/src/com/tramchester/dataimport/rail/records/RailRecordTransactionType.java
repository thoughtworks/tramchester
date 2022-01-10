package com.tramchester.dataimport.rail.records;

import java.util.HashMap;

public enum RailRecordTransactionType {
    New("N"),
    Delete("D"),
    Revise("R"),
    Unknown("unknown");

    private static final HashMap<String, RailRecordTransactionType> map = new HashMap<>();

    static {
        for(RailRecordTransactionType transactionType : RailRecordTransactionType.values()) {
            map.put(transactionType.code, transactionType);
        }
    }

    private final String code;

    RailRecordTransactionType(String code) {
        this.code = code;
    }

    public static RailRecordTransactionType parse(String text) {
        if (map.containsKey(text)) {
            return map.get(text);
        }
        return Unknown;
    }
}
