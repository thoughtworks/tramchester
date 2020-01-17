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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);
    private TimeZone timeZone = TimeZone.getTimeZone(TramchesterConfig.TimeZone);
    private final StationRepository stationRepository;

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
                result.add(parseItem((JSONObject) anInfoList));
            }
        }
        return result;
    }

    private StationDepartureInfo parseItem(JSONObject jsonObject) {
        logger.debug(format("Parsing JSON '%s'", jsonObject));
        Long displayId = (Long) jsonObject.get("Id");
        String lineName = (String) jsonObject.get("Line");
        String stationPlatform = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        String location = (String)jsonObject.get("StationLocation");
        String rawDirection = (String)jsonObject.get("Direction");
        StationDepartureInfo.Direction direction = StationDepartureInfo.Direction.valueOf(rawDirection);

        LocalDateTime updateTime = getStationUpdateTime(dateString);
        logger.debug("Parsed lived data with update time: "+updateTime);
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId.toString(), lineName, direction, stationPlatform,
                location, message, updateTime);
        parseDueTrams(jsonObject,departureInfo);
        logger.debug("Parsed live data to " + departureInfo);
        return departureInfo;
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
            destinationName = mapNames(departureInfo, destinationName);
            Optional<Station> maybeStation = stationRepository.getStationByName(destinationName);
            if (!maybeStation.isPresent()) {
                if ( ! (destinationName.isEmpty() || destinationName.equals("See Tram Front")) ) {
                    logger.warn(format("Unable to map destination name: '%s'", destinationName));
                }
            }

            maybeStation.ifPresent(station ->  {
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

    private String mapNames(StationDepartureInfo departureInfo, String destinationName) {
        if ("Firswood".equals(destinationName)) {
            destinationName = "Firswood Station";
        }
        if ("Terminates Here".equals(destinationName)) {
            destinationName = departureInfo.getLocation();
        }
        return destinationName;
    }

    private String getNumberedField(JSONObject jsonObject, String name, final int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
