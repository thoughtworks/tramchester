package com.tramchester.mappers;

import java.util.HashMap;
import java.util.Map;

public class HeadsignMapper {
    Map<String, String> map;

    public HeadsignMapper() {
        map = new HashMap<>();
        map.put("Deansgate-Castlefield", "Deansgate Castlefield");
        map.put("Rochdale Interchange","Rochdale Town Centre");
    }

    public String mapToDestination(String headSign) {
        if (map.containsKey(headSign)) {
            return map.get(headSign);
        }
        return headSign;
    }
}
