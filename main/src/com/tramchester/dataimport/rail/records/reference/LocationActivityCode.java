package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import java.util.Arrays;
import java.util.List;

public enum LocationActivityCode implements EnumMap.HasCodes<LocationActivityCode> {
    StopsToTakeUpAndSetDownPassengers("T"),
    TrainBegins("TB"),
    TrainFinishes("TF"),
    StopsToTakeUpPassengers("U"),
    StopsToSetDownPassengers("D"),
    StopsWhenRequired("R"),

    StopsToDetachVehicles("-D"),
    StopsToAttachAndDetachVehicles("-T"),
    StopsToAttachVehicles("-U"),
    StopsOrShuntsForOtherTrainsToPass("A"),
    AttachDetachAssistingLocomotive("AE"),
    ShowsAsXOnArrival("AX"),
    StopsForBankingLocomotive("BL"),
    StopsToChangeTrainCrew("C"),
    StopsForExamination("E"),
    GBPRTTDataToAdd("G"),
    NotionalActivityToPreventWTTColumnsMerge("H"),
    ToPreventWTTColumnMergeWhere3rdColumn("HH"),
    PassengerCountPoint("K"),
    TicketCollectionAndExaminationPoint("KC"),
    TicketExaminationPoint("KE"),
    TicketExaminationPointFirstClassOnly("KF"),
    SelectiveTicketExaminationPoint("KS"),
    StopsToChangeLocomotive("L"),
    StopNotAdvertised("N"),
    StopsForOtherOperatingReasons("OP"),
    TrainLocomotiveOnRear("OR"),
    PropellingBetweenPointsShown("PR"),
    StopsForReversingMoveOrDriverChangesEnds("RM"),
    StopsForLocomotiveToRunRoundTrain("RR"),
    StopsForRailwayPersonnelOnly("S"),
    ActivityRequestedForTOPSReportingPurposes("TS"),
    StopsOrPassesForTabletStaffOrToken("TW"),
    StopsForWateringOfCoaches("W"),
    PassesAnotherTrainAtCrossingPointOnASingleLine("X"),

    // additional codes added
    TrainBeginsTakeUp("TBT"),
    TrainFinishesSetDown("TFT"),
    Unknown("UNKNOWN"),
    NONE("NONE");

    private static final EnumMap<LocationActivityCode> map = new EnumMap<>(LocationActivityCode.values());
    private static final List<LocationActivityCode> dropOffs = Arrays.asList(TrainFinishesSetDown, TrainFinishes,
            StopsToSetDownPassengers, StopsToTakeUpAndSetDownPassengers);
    private static final List<LocationActivityCode> pickUps = Arrays.asList(TrainBeginsTakeUp, TrainBegins,
            StopsToTakeUpPassengers, StopsToTakeUpAndSetDownPassengers);
    private final String code;

    LocationActivityCode(String code) {
        this.code = code;
    }

    public static LocationActivityCode parse(String code) {
        String lookup = code.trim();
        if (lookup.isEmpty()) {
            return NONE;
        }

        LocationActivityCode result = map.get(lookup);
        if (result==null) {
            return Unknown;
        }
        return result;
    }

    @Override
    public String getCode() {
        return code;
    }

    // TODO Request Stops?

    public boolean isDropOff() {
        return dropOffs.contains(this);
    }

    public boolean isPickup() {
        return pickUps.contains(this);
    }
}
