package com.tramchester.mappers;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

import static com.tramchester.domain.liveUpdates.StationDepartureInfo.Direction.Both;
import static com.tramchester.domain.liveUpdates.StationDepartureInfo.Direction.Unknown;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);

    private static final String DIRECTION_BOTH = "Incoming/Outgoing";
    private static final String TERMINATES_HERE = "Terminates Here";
    private TimeZone timeZone = TimeZone.getTimeZone(TramchesterConfig.TimeZone);
    private final StationRepository stationRepository;
    private static List<String> NotADestination = Arrays.asList("See Tram Front", "Not in Service");

    // live data api has limit in number of results
    private int MAX_DUE_TRAMS = 4;

    public LiveDataParser(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<StationDepartureInfo> parse(String rawJson) {
        List<StationDepartureInfo> result = new LinkedList<>();

        //JsonParser jsonParser = new JSONParser();
        JsonObject parsed = Jsoner.deserialize(rawJson, new JsonObject()); //(JsonObject)jsonParser.parse(rawJson);
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
        String lineName = (String) jsonObject.get("Line");
        String atcoCode = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        String rawDirection = (String)jsonObject.get("Direction");

        StationDepartureInfo.Direction direction = getDirection(rawDirection);
        if (direction==Unknown) {
            logger.warn("Unable to map direction code name "+ rawDirection + " for JSON " +jsonObject.toString());
        }
        Optional<Station> maybeStation = getStationByAtcoCode(atcoCode);
        if (maybeStation.isEmpty()) {
            logger.warn("Unable to map atco code name "+ atcoCode + " for JSON " +jsonObject.toString());
            return Optional.empty();
        }

        LocalDateTime updateTime = getStationUpdateTime(dateString);
        logger.debug("Parsed lived data with update time: "+updateTime);
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId.toString(), lineName, direction,
                atcoCode, maybeStation.get(), message, updateTime);
        parseDueTrams(jsonObject, departureInfo);

        logger.debug("Parsed live data to " + departureInfo);
        return Optional.of(departureInfo);
    }

    private StationDepartureInfo.Direction getDirection(String rawDirection) {
        if (DIRECTION_BOTH.equals(rawDirection)) {
            return Both;
        }
        try {
            return StationDepartureInfo.Direction.valueOf(rawDirection);
        }
        catch (IllegalArgumentException unexpectedValueInTheApi) {
            logger.warn("Unable to parse " + rawDirection);
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
            if (!destinationName.isEmpty()) {
                if (TERMINATES_HERE.equals(destinationName)) {
                    destinationName = departureInfo.getLocation();
                }
                Optional<Station> maybeStation = getTramDestination(destinationName);

                maybeStation.ifPresent(station -> {
                    String status = getNumberedField(jsonObject, "Status", index);
                    String waitString = getNumberedField(jsonObject, "Wait", index);
                    int wait = Integer.parseInt(waitString);
                    String carriages = getNumberedField(jsonObject, "Carriages", index);
                    LocalTime lastUpdate = departureInfo.getLastUpdate().toLocalTime();

                    DueTram dueTram = new DueTram(station, status, wait, carriages, lastUpdate);
                    departureInfo.addDueTram(dueTram);
                });
            }
        }
    }

    private Optional<Station> getStationByAtcoCode(String atcoCode) {
        String stationId = Station.formId(atcoCode);
        if (stationRepository.hasStationId(stationId)) {
            return Optional.of(stationRepository.getStation(stationId));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Station> getTramDestination(String name) {
        String destinationName = mapLiveAPIToTimetableDataNames(name);
        if (destinationName.isEmpty())
        {
            logger.warn("Got empty name");
            return Optional.empty();
        }
        if (NotADestination.contains(destinationName)) {
            logger.debug(format("Not mapping destination name: '%s'", destinationName));
            return Optional.empty();
        }

        return stationRepository.getStationByName(destinationName);
    }

    private String mapLiveAPIToTimetableDataNames(String destinationName) {
        int viaIndex = destinationName.toLowerCase().indexOf(" via");
        if (viaIndex > 0) {
            destinationName = destinationName.substring(0, viaIndex);
        }
        if ("Firswood".equals(destinationName)) {
            return "Firswood Station";
        }
        if ("Deansgate - Castlefield".equals(destinationName)) {
            return "Deansgate-Castlefield";
        }
        if ("Besses O’ Th’ Barn".equals(destinationName)) {
            return "Besses o'th'barn";
        }
        if ("Newton Heath and Moston".equals(destinationName)) {
            return "Newton Heath & Moston";
        }
        if ("St Werburgh’s Road".equals(destinationName)) {
            return "St Werburgh's Road";
        }
        return destinationName;
    }

    private String getNumberedField(JsonObject jsonObject, String name, final int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
