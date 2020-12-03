package com.tramchester.domain.reference;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.places.Station;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.domain.liveUpdates.Lines.*;

public enum CentralZoneStations {
    Cornbrook("9400ZZMACRN", Eccles),
    StPetersSquare("9400ZZMASTP", Eccles),
    PiccadillyGardens("9400ZZMAPGD", Bury),
    TraffordBar("9400ZZMATRA", Altrincham),
    StWerbergsRoad("9400ZZMASTW", SouthManchester),
    Victoria("9400ZZMAVIC", Altrincham),
    Deansgate("9400ZZMAGMX", Altrincham),
    Piccadilly("9400ZZMAPIC", Bury),
    MarketStreet("9400ZZMAMKT", Bury),
    Firswood("9400ZZMAFIR", SouthManchester),
    Shudehill("9400ZZMASHU", Altrincham),
    Pomona("9400ZZMAPOM", Eccles),
    Chorlton("9400ZZMACHO", SouthManchester),
    ExchangeSquare("9400ZZMAEXS", Eccles);

    private final IdFor<Station> stationId;
    private final Lines line;

    CentralZoneStations(String stationId, Lines line) {
        this.stationId = createId(stationId);
        this.line = line;
    }

    public static boolean contains(Station current) {
        return ids.contains(current.getId());
    }

    public boolean matches(Station station) {
        return stationId.equals(station.getId());
    }

    public IdFor<Station> getId() {
        return stationId;
    }

    public static IdSet<Station> ids;
    public static final Map<IdFor<Station>, CentralZoneStations> map;

    static {
        ids = Arrays.stream(CentralZoneStations.values()).map(CentralZoneStations::getId).collect(IdSet.idCollector());
        map = Arrays.stream(CentralZoneStations.values()).map(element -> Pair.of(element.getId(),element)).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private IdFor<Station> createId(String stationId) {
        return IdFor.createId(stationId);
    }

    public Lines getLine() {
        return line;
    }
}
