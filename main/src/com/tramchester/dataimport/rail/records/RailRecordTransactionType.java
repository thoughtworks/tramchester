package com.tramchester.dataimport.rail.records;

import java.util.HashMap;

public enum RailRecordTransactionType {
    New('N'),
    Delete('D'),
    Revise('R'),
    Unknown('Z');

    private static final HashMap<Character, RailRecordTransactionType> map = new HashMap<>();

    static {
        for(RailRecordTransactionType transactionType : RailRecordTransactionType.values()) {
            map.put(transactionType.code, transactionType);
        }
    }

    private final char code;

    RailRecordTransactionType(char code) {
        this.code = code;
    }

    public static RailRecordTransactionType parse(char theChar) {
        if (map.containsKey(theChar)) {
            return map.get(theChar);
        }
        return Unknown;
    }
}
