package com.tramchester.mappers;

import com.tramchester.domain.DueTram;
import com.tramchester.domain.StationDepartureInfo;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class LiveDataMapper {

    private int MAX_DUE_TRAMS = 4;

    public List<StationDepartureInfo> map(String rawJson) throws ParseException {
        List<StationDepartureInfo> result = new LinkedList<>();

        JSONParser jsonParser = new JSONParser();
        JSONObject parsed = (JSONObject)jsonParser.parse(rawJson);
        JSONArray infoList = (JSONArray) parsed.get("value");

        infoList.forEach(item -> {
            result.add(parseItem((JSONObject) item));
        });

        return result;
    }

    private StationDepartureInfo parseItem(JSONObject jsonObject) {
        String lineName = (String) jsonObject.get("Line");
        String stationPlatform = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        DateTime lastUpdate = DateTime.parse(dateString);
        StationDepartureInfo departureInfo = new StationDepartureInfo(lineName, stationPlatform, message, lastUpdate);
        parseDueTrams(jsonObject,departureInfo);
        return departureInfo;
    }

    private void parseDueTrams(JSONObject jsonObject, StationDepartureInfo departureInfo) {
        for (int i = 0; i < MAX_DUE_TRAMS; i++) {
            String dest = getField(jsonObject, i, "Dest");
            if (dest.length()>0) {
                String status = getField(jsonObject, i, "Status");
                String waitString = getField(jsonObject, i, "Wait");
                int wait = Integer.parseInt(waitString);
                String carriages = getField(jsonObject, i, "Carriages");
                DueTram dueTram = new DueTram(dest, status, wait, carriages);
                departureInfo.addDueTram(dueTram);
            }
        }
    }

    private String getField(JSONObject jsonObject, int i, String name) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
