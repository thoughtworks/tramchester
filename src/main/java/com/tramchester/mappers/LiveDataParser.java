package com.tramchester.mappers;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);

    private int MAX_DUE_TRAMS = 4;

    public List<StationDepartureInfo> parse(String rawJson) throws ParseException {
        List<StationDepartureInfo> result = new LinkedList<>();

        JSONParser jsonParser = new JSONParser();
        JSONObject parsed = (JSONObject)jsonParser.parse(rawJson);
        JSONArray infoList = (JSONArray) parsed.get("value");

        if (infoList!=null) {
            for (int i = 0; i < infoList.size(); i++) {
                result.add(parseItem((JSONObject) infoList.get(i)));
            }
        }

        return result;
    }

    private StationDepartureInfo parseItem(JSONObject jsonObject) {
        Long displayId = (Long) jsonObject.get("Id");
        String lineName = (String) jsonObject.get("Line");
        String stationPlatform = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        String location = (String)jsonObject.get("StationLocation");
        Instant lastUpdate = Instant.from(ISO_INSTANT.parse(dateString));
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId.toString(), lineName, stationPlatform,
                location, message, lastUpdate.atZone(ZoneId.of("Z")).toLocalDateTime());
        parseDueTrams(jsonObject,departureInfo);
        return departureInfo;
    }

    private void parseDueTrams(JSONObject jsonObject, StationDepartureInfo departureInfo) {
        for (int i = 0; i < MAX_DUE_TRAMS; i++) {
            String dest = getNumberedField(jsonObject, "Dest", i);
            if (dest.length()>0) {
                String status = getNumberedField(jsonObject, "Status", i);
                String waitString = getNumberedField(jsonObject, "Wait", i);
                int wait = Integer.parseInt(waitString);
                String carriages = getNumberedField(jsonObject, "Carriages", i);
                LocalDateTime lastUpdate = departureInfo.getLastUpdate();
                DueTram dueTram = new DueTram(dest, status, wait, carriages, lastUpdate.toLocalTime());
                departureInfo.addDueTram(dueTram);
            }
        }
    }

    private String getNumberedField(JSONObject jsonObject, String name, int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
