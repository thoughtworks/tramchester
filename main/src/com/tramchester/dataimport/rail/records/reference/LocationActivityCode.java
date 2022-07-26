package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

public enum LocationActivityCode implements EnumMap.HasCodes {
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

    private static final List<Integer> codeLengths;
    private static final EnumMap<LocationActivityCode> codes;

    static {
        codes = new EnumMap<>(values());

        Set<Integer> uniqueLengths = Arrays.stream(values()).map(item -> item.code.length()).
                filter(size -> size>0).
                collect(Collectors.toSet());
        // need the longest match first
        codeLengths = Lists.reverse(uniqueLengths.stream().sorted(Integer::compare).collect(Collectors.toList()));
    }

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
        String lookup = code.trim();
        if (lookup.isEmpty()) {
            return EnumSet.noneOf(LocationActivityCode.class);
        }

        return getCodesFor(lookup);

    }

    private static EnumSet<LocationActivityCode> getCodesFor(final String text) {
        EnumSet<LocationActivityCode> result = EnumSet.noneOf(LocationActivityCode.class);

        String toProcess = text;

        int index = 0;
        while(index<codeLengths.size()) {
            final int codeLength = codeLengths.get(index);
            if (codeLength > toProcess.length()) {
                index++;
            } else {
                final String candidate = toProcess.substring(0, codeLength);
                if (codes.containsCode(candidate)) {
                    toProcess = toProcess.substring(codeLength).trim();
                    result.add(codes.get(candidate));
                } else {
                    index++;
                }
            }
        }

        return result;

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

    //@Override
    public String getCode() {
        return code;
    }

}
