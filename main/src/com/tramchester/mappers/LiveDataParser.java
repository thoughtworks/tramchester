package com.tramchester.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.repository.StationRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);
    public static final String TERMINATES_HERE = "Terminates Here";
    private TimeZone timeZone = TimeZone.getTimeZone(TramchesterConfig.TimeZone);
    private final StationRepository stationRepository;
    private static List<String> NotDestination = Arrays.asList("See Tram Front", "Not in Service");

    // live data api has limit in number of results
    private int MAX_DUE_TRAMS = 4;

    public LiveDataParser(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<StationDepartureInfo> parse(String rawJson) throws ParseException {
        List<StationDepartureInfo> result = new LinkedList<>();

        JSONParser jsonParser = new JSONParser();
        JSONObject parsed = (JSONObject)jsonParser.parse(rawJson);
        JSONArray infoList = (JSONArray) parsed.get("value");

        if (infoList!=null) {
            for (Object anInfoList : infoList) {
                Optional<StationDepartureInfo> item = parseItem((JSONObject) anInfoList);
                item.ifPresent(result::add);
            }
        }
        return result;
    }

    private Optional<StationDepartureInfo> parseItem(JSONObject jsonObject) {
        logger.debug(format("Parsing JSON '%s'", jsonObject));

        Long displayId = (Long) jsonObject.get("Id");
        String lineName = (String) jsonObject.get("Line");
        String atcoCode = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        //String rawlocation = (String)jsonObject.get("StationLocation");
        String rawDirection = (String)jsonObject.get("Direction");

        StationDepartureInfo.Direction direction = StationDepartureInfo.Direction.valueOf(rawDirection);
        Optional<Station> maybeStation = getStationByAtcoCode(atcoCode);
        if (!maybeStation.isPresent()) {
            logger.warn("Unable to map atco code name "+ atcoCode);
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

    private void parseDueTrams(JSONObject jsonObject, StationDepartureInfo departureInfo) {
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
        return stationRepository.getStation(stationId);
    }

    private Optional<Station> getTramDestination(String name) {
        String destinationName = mapLiveAPIToTimetableDataNames(name);
        if (destinationName.isEmpty() || NotDestination.contains(destinationName)) {
            logger.info(format("Unable to map destination name: '%s'", destinationName));
            return Optional.empty();
        }

        return stationRepository.getStationByName(destinationName);
    }

    private String mapLiveAPIToTimetableDataNames(String destinationName) {
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

    private String getNumberedField(JSONObject jsonObject, String name, final int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
