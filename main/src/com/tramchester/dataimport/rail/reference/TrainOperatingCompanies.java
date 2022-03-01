package com.tramchester.dataimport.rail.reference;

import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// useful source http://www.railwaycodes.org.uk/operators/toccodes.shtm

public enum TrainOperatingCompanies {
    IL("Island Line"),
    NT("Northern"),
    SW("South Western Railway"),
    CH("Chiltern Railways"),
    GN("Great Northern"),
    EM("East Midlands Railway"),
    GR("LNER"),
    XC("CrossCountry"),
    CS("Caledonian Sleeper"),
    GW("Great Western Railway"),
    GX("Gatwick Express"),
    AW("Arriva Trains Wales"),
    LT("London Underground"),
    LD("Lumo"),
    lE("Greater Anglia"),
    XR("Crossrail"),
    TP("TransPennine Express"),
    VT("Avanti West Coast"),
    LM("West Midlands Railway"),
    LO("London Overground"),
    TL("Thameslink"),
    LE("Greater Anglia"),
    HT("Hull Trains"),
    HX("Heathrow Express"),
    SE("Southeastern"),
    ME("Merseyrail"),
    SN("Southern"),
    GC("Grand Central"),
    SR("ScotRail"),
    CC("c2c"),
    UNKNOWN("Train");

    private final String companyName;
    private static final Map<IdFor<Agency>, String> nameMap = new HashMap<>();

    static {
        Arrays.stream(TrainOperatingCompanies.values()).forEach(value -> nameMap.put(StringIdFor.createId(value.name()),
                value.companyName));
    }

    TrainOperatingCompanies(String companyName) {
        this.companyName = companyName;
    }

    public static String nameFor(IdFor<Agency> id) {
        if (!nameMap.containsKey(id)) {
            return UNKNOWN.companyName;
        }
        return nameMap.get(id);
    }

    public String getName() {
        return companyName;
    }
}
