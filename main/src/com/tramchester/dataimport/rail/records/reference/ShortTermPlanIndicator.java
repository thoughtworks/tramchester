package com.tramchester.dataimport.rail.records.reference;

public enum ShortTermPlanIndicator implements EnumMap.HasCodes {
        Cancellation('C'),
        New('N'),
        Overlay('O'),
        Unknown('Z'),
        Permanent('P');

    private static final EnumMap<ShortTermPlanIndicator> codes = new EnumMap<>(ShortTermPlanIndicator.values());

    private final char code;

    ShortTermPlanIndicator(char code) {
        this.code = code;
    }

    public static ShortTermPlanIndicator getFor(char code) {
        return codes.get(code);
    }

    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
