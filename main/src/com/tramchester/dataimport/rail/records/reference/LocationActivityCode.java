package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import com.google.common.collect.Sets;

import java.util.*;

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
    None("");

    private static final EnumSet<LocationActivityCode> dropOffs = EnumSet.of(TrainFinishes, StopsToSetDownPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);
    private static final EnumSet<LocationActivityCode> pickUps = EnumSet.of(TrainBegins, StopsToTakeUpPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private static final EnumSet<LocationActivityCode> stops = EnumSet.of(TrainBegins, TrainFinishes,
            StopsToSetDownPassengers, StopsToTakeUpPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private final String code;

    LocationActivityCode(String code) {
        this.code = code;
    }

    public static EnumSet<LocationActivityCode> parse(String code) {
        EnumSet<LocationActivityCode> result = EnumSet.noneOf(LocationActivityCode.class);
        String lookup = code.trim();
        if (lookup.isEmpty()) {
            return EnumSet.noneOf(LocationActivityCode.class);
        }

        // most cases once
        while (!lookup.isEmpty()) {
            LocationActivityCode found = getLongestMatch(lookup);
            if (found==None) {
                lookup = lookup.substring(1);
            } else {
                lookup = lookup.replaceFirst(found.code, "");
                result.add(found);
            }
        }

        return result;
    }

    private static LocationActivityCode getLongestMatch(String text) {
        Optional<LocationActivityCode> found = Arrays.stream(LocationActivityCode.values()).
                filter(item -> text.startsWith(item.code)).
                max(Comparator.comparingInt(a -> a.code.length()));
        return found.orElse(None);
    }

    public static boolean doesStop(EnumSet<LocationActivityCode> activity) {
        return !Sets.intersection(activity, stops).isEmpty();
    }

    public static boolean doesPickup(EnumSet<LocationActivityCode> activity) {
        return !Sets.intersection(activity, pickUps).isEmpty();
    }

    public static boolean doesDropOff(EnumSet<LocationActivityCode> activity) {
        return !Sets.intersection(activity, dropOffs).isEmpty();
    }

    @Override
    public String getCode() {
        return code;
    }

}
