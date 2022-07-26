package com.tramchester.dataimport.rail.records.reference;

// Interchange Status. Values:
// 0 Not an interchange Point
// 1 Small Interchange Point
// 2 Medium Interchange Point
// 3 Large Interchange Point
// 9 This is a subsidiary TIPLOC at a station which has more than one TIPLOC. Stations which have more than one
//      TIPLOC always have the same principal 3-Alpha Code.

public enum RailInterchangeType implements EnumMap.HasCodes {
    None('0'),
    Small('1'),
    Medium('2'),
    Large('3'),
    Subsidiary('9');

    private static final EnumMap<RailInterchangeType> codes = new EnumMap<>(RailInterchangeType.values());

    private final char typeCode;

    RailInterchangeType(char typeCode) {
        this.typeCode = typeCode;
    }

    public static RailInterchangeType getFor(char aChar) {
        if (aChar==' ') {
            return RailInterchangeType.None;
        }
        return codes.get(aChar);
    }

    @Override
    public String getCode() {
        return String.valueOf(typeCode);
    }
}
