package com.tramchester.mappers;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.LineDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

import static com.tramchester.domain.liveUpdates.LineDirection.Both;
import static com.tramchester.domain.liveUpdates.LineDirection.Unknown;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

@LazySingleton
public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);

    private static final String DIRECTION_BOTH = "Incoming/Outgoing";
    private static final String TERMINATES_HERE = "Terminates Here";
    private static final String NOT_IN_SERVICE = "Not in Service";
    private final TimeZone timeZone = TimeZone.getTimeZone(TramchesterConfig.TimeZone);
    private final StationRepository stationRepository;
    private static final List<String> NotADestination = Arrays.asList("See Tram Front", NOT_IN_SERVICE);
    private final Map<String, String> mapping;

    // live data api has limit in number of results
    private static final int MAX_DUE_TRAMS = 4;

    public enum LiveDataNamesMapping {
        Firswood("Firswood", "Firswood Station"),
        Ashton("Ashton","Ashton-Under-Lyne"),
        DeansgateAliasA("Deansgate - Castlefield","Deansgate-Castlefield"),
        DeansgateAliasB("Deansgate Castlefield","Deansgate-Castlefield"),
        BessesOThBarns("Besses O’ Th’ Barn","Besses o'th'barn"),
        NewtonHeathAndMoston("Newton Heath and Moston","Newton Heath & Moston"),
        StWerburgsRoad("St Werburgh’s Road","St Werburgh's Road"),
        Rochdale("Rochdale Stn", "Rochdale Railway Station");

        private final String from;
        private final String too;

        LiveDataNamesMapping(String from, String too) {
            this.from = from;
            this.too = too;
        }

        public String getToo() {
            return too;
        }
    }

    @Inject
    public LiveDataParser(StationRepository stationRepository) {
        mapping = new HashMap<>();
        List<LiveDataNamesMapping> refereceData = Arrays.asList(LiveDataNamesMapping.values());
        refereceData.forEach(item -> mapping.put(item.from, item.too));
        this.stationRepository = stationRepository;
    }

    public List<StationDepartureInfo> parse(String rawJson) {
        List<StationDepartureInfo> result = new LinkedList<>();

        JsonObject parsed = Jsoner.deserialize(rawJson, new JsonObject());
        if (parsed.containsKey("value")) {
            JsonArray infoList = (JsonArray ) parsed.get("value");

            if (infoList!=null) {
                for (Object anInfoList : infoList) {
                    Optional<StationDepartureInfo> item = parseItem((JsonObject) anInfoList);
                    item.ifPresent(result::add);
                }
            }
        } else {
            logger.error("Unable to deserialise received json: "+rawJson);
        }

        return result;
    }

    private Optional<StationDepartureInfo> parseItem(JsonObject jsonObject) {
        logger.debug(format("Parsing JSON '%s'", jsonObject));

        BigDecimal displayId = (BigDecimal) jsonObject.get("Id");
        String rawLine = (String) jsonObject.get("Line");
        String atcoCode = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        String rawDirection = (String)jsonObject.get("Direction");

        LineDirection direction = getDirection(rawDirection);
        if (direction==Unknown) {
            logger.warn("Display '" + displayId +"' Unable to map direction code name "+ rawDirection + " for JSON " +jsonObject.toString());
        }

        Lines line = getLine(rawLine);
        if (line== Lines.UnknownLine) {
            logger.warn("Display '" + displayId +"' Unable to map line name "+ rawLine + " for JSON " +jsonObject.toString());
        }

        Optional<Station> maybeStation = getStationByAtcoCode(atcoCode);
        if (maybeStation.isEmpty()) {
            logger.warn("Display '" + displayId + "' Unable to map atco code to station '"+ atcoCode + "' for JSON " +jsonObject.toString());
            return Optional.empty();
        }
        Station station = maybeStation.get();

        LocalDateTime updateTime = getStationUpdateTime(dateString);
        logger.debug("Parsed lived data with update time: "+updateTime);

        IdFor<Platform> platformId = IdFor.createId(atcoCode);
        if (!station.hasPlatform(platformId)) {
            // info not warn as currently a permanent issue with the data
            logger.info(format("Display '%s' Platform '%s' not in timetable data for station %s and Json %s",
                    displayId, atcoCode, station.getId(), jsonObject.toString()));
            // Seems to be legit, at very least some single platform stations (i.e. navigation road) appear to have
            // two platforms in the live data feed...
            //return Optional.empty();
        }

        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId.toString(), line, direction,
                platformId, station, message, updateTime);
        parseDueTrams(jsonObject, departureInfo);

        logger.debug("Parsed live data to " + departureInfo);
        return Optional.of(departureInfo);
    }

    private Lines getLine(String text) {
        Lines[] valid = Lines.values();
        for (Lines line : valid) {
            if (line.getName().equals(text)) {
                return line;
            }
        }
        return Lines.UnknownLine;
    }

    private LineDirection getDirection(String text) {
        if (DIRECTION_BOTH.equals(text)) {
            return Both;
        }
        try {
            return LineDirection.valueOf(text);
        }
        catch (IllegalArgumentException unexpectedValueInTheApi) {
            logger.warn("Unable to parse direction " + text);
        }
        return Unknown;
    }

    private LocalDateTime getStationUpdateTime(String dateString) {
        Instant instanceOfUpdate = Instant.from(ISO_INSTANT.parse(dateString));

        ZonedDateTime zonedDateTime = instanceOfUpdate.atZone(TramchesterConfig.TimeZone);
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

        // WORKAROUND - feed always contains 'Z' at end of date/time even though feed actually switches to BST
        boolean dst = timeZone.inDaylightTime(Date.from(instanceOfUpdate));
        if (dst) {
            localDateTime = localDateTime.minusSeconds(timeZone.getDSTSavings()/1000);
        }

        return localDateTime;
    }

    private void parseDueTrams(JsonObject jsonObject, StationDepartureInfo departureInfo) {
        for (int i = 0; i < MAX_DUE_TRAMS; i++) {
            final int index = i;
            String destinationName = getNumberedField(jsonObject, "Dest", index);
            if (destinationName.isEmpty()) {
                // likely not present in json
                logger.debug("Skipping destination '" + destinationName + "' for " + jsonObject.toString() + " and index " + i);
            } else if (NotADestination.contains(destinationName)) {
                logger.info("Display '" + departureInfo.getDisplayId() + "' Skipping destination '" + destinationName + "' for " + jsonObject.toJson() + " and index " + i);
            } else {
                Optional<Station> maybeDestStation;
                if (TERMINATES_HERE.equals(destinationName)) {
                    // replace "terminates here" with the station where this message is displayed
                    maybeDestStation = Optional.of(departureInfo.getStation());
                } else {
                    // try to look up destination station based on the destination text....
                    maybeDestStation = getTramDestination(destinationName);
                }

                maybeDestStation.ifPresentOrElse(station -> {
                            String status = getNumberedField(jsonObject, "Status", index);
                            String waitString = getNumberedField(jsonObject, "Wait", index);
                            int wait = Integer.parseInt(waitString);
                            String carriages = getNumberedField(jsonObject, "Carriages", index);
                            LocalTime lastUpdate = departureInfo.getLastUpdate().toLocalTime();

                            DueTram dueTram = new DueTram(station, status, wait, carriages, lastUpdate);
                            departureInfo.addDueTram(dueTram);
                        },

                        () -> logger.warn("Display id '" + departureInfo.getDisplayId() + "' Unable to match due tram destination '" + destinationName + "' index: " + index +" json '"+jsonObject+"'"));
            }
        }
    }

    private Optional<Station> getStationByAtcoCode(String atcoCode) {
        IdFor<Station> stationId = Station.formId(atcoCode);
        if (stationRepository.hasStationId(stationId)) {
            return Optional.of(stationRepository.getStationById(stationId));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Station> getTramDestination(String name) {
        if (name.isEmpty())
        {
            logger.warn("Got empty name");
            return Optional.empty();
        }
        if (NotADestination.contains(name)) {
            logger.info(format("Not a destination: '%s'", name));
            return Optional.empty();
        }

        String destinationName = mapLiveAPIToTimetableDataNames(name);
        return stationRepository.getTramStationByName(destinationName);
    }

    private String mapLiveAPIToTimetableDataNames(String destinationName) {
        destinationName = destinationName.replace("Via", "via");

        // assume station name is valid.....
        int viaIndex = destinationName.indexOf(" via");
        if (viaIndex > 0) {
            destinationName = destinationName.substring(0, viaIndex);
        }

        if (mapping.containsKey(destinationName)) {
            return mapping.get(destinationName);
        }

        return destinationName;
    }

    private String getNumberedField(JsonObject jsonObject, String name, final int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
