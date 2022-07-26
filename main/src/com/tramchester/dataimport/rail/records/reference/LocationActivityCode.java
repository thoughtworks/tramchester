package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.checkerframework.checker.units.qual.N;

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

    private static final EnumMap<LocationActivityCode> codes  = new EnumMap<>(values());

    // TrainBegins and TrainFinishes seem to be used inconsistently,
    // i.e. TF is not always paired with T even when train does actually drop off passengers

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

        String[] tokens = text.split(" ");
        for (String token : tokens) {
            result.addAll(parseToken(token));
        }
        return result;

    }

    private static EnumSet<LocationActivityCode> parseToken(String token) {
        EnumSet<LocationActivityCode> result = EnumSet.noneOf(LocationActivityCode.class);

        String toProcess = token;

        while (!toProcess.isEmpty()) {
            int len = Math.min(toProcess.length(), 2);
            LocationActivityCode attempt = parseSingle(toProcess.substring(0,len));
            if (attempt == None) {
                len = Math.min(toProcess.length(), 1);
                attempt = parseSingle(toProcess.substring(0, len));
            }
            if (attempt == None) {
                toProcess = toProcess.substring(2);
            } else {
                result.add(attempt);
                toProcess = toProcess.substring(attempt.code.length());
            }
        }
        return result;

    }

    private static LocationActivityCode parseSingle(String text) {
        if (codes.containsCode(text)) {
            return codes.get(text);
        }
        return None;
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
