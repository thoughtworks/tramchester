package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import java.util.Arrays;
import java.util.List;

public enum LocationActivityCode implements EnumMap.HasCodes<LocationActivityCode> {
    StopsToDetachVehicles("-D"),
    StopsToAttachAndDetachVehicles("-T"),
    StopsToAttachVehicles("-U"),
    StopsOrShuntsForOtherTrainsToPass("A"),
    AttachDetachAssistingLocomotive("AE"),
    ShowsAsXOnArrival("AX"),
    StopsForBankingLocomotive("BL"),
    StopsToChangeTrainCrew("C"),
    StopsToSetDownPassengers("D"),
    StopsForExamination("E"),
    GBPRTTDataToAdd("G"),
    NotionalActivityToPreventWTTColumnsMerge("H"),
    ToPreventWTTColumnMergeWhere3rdColumn("HH"),
    PassengerCountPoint("K"),
    Ticketcollectionandexaminationpoint("KC"),
    Ticketexaminationpoint("KE"),
    TicketexaminationpointFirstclassonly("KF"),
    SelectiveTicketExaminationPoint("KS"),
    Stopstochangelocomotive("L"),
    Stopnotadvertised("N"),
    Stopsforotheroperatingreasons("OP"),
    TrainLocomotiveonrear("OR"),
    Propellingbetweenpointsshown("PR"),
    StopsWhenRequired("R"),
    Stopsforreversingmoveordriverchangesends("RM"),
    Stopsforlocomotivetorunroundtrain("RR"),
    StopsForRailwayPersonnelOnly("S"),
    StopsToTakeUpAndSetDownPassengers("T"),
    TrainBegins("TB"),
    TrainFinishes("TF"),
    ActivityrequestedforTOPSreportingpurposes("TS"),
    StopsOrPassesForTabletStaffOrToken("TW"),
    StopsToTakeupPassengers("U"),
    Stopsforwateringofcoaches("W"),
    Passesanothertrainatcrossingpointonasingleline("X"),

    // additional codes added
    TrainBeginsTakeUp("TBT"),
    TrainFinishesSetDown("TFT"),
    Unknown("UNKNOWN"),
    NONE("NONE");

    private static final EnumMap<LocationActivityCode> map = new EnumMap<>(LocationActivityCode.values());
    private static final List<LocationActivityCode> dropOffs = Arrays.asList(TrainFinishesSetDown, TrainFinishes,
            StopsToSetDownPassengers, StopsToTakeUpAndSetDownPassengers);
    private static final List<LocationActivityCode> pickUps = Arrays.asList(TrainBeginsTakeUp, TrainBegins,
            StopsToTakeupPassengers, StopsToTakeUpAndSetDownPassengers);
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
