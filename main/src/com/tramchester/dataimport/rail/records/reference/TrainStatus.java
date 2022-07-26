package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php/CIF_Codes

public enum TrainStatus implements EnumMap.HasCodes {
    Bus('B'),
    Freight('F'),
    PassengerAndParcels('P'),
    Ship('S'),
    Trip('T'),
    STPPassengerParcels('1'),
    STPFreight('2'),
    STPTrip('3'),
    STPShip('4'),
    STPBus('5'),
    Unknown('Z');

    private static final EnumMap<TrainStatus> codes = new EnumMap<>(TrainStatus.values());

    private final char code;

    TrainStatus(char code) {
        this.code = code;
    }

    public static TrainStatus getFor(char code) {
        return codes.get(code);
    }

    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
