package com.tramchester.mappers;

import java.util.HashMap;
import java.util.Map;

public class HeadsignMapper {
    private final Map<String, String> map;

    public HeadsignMapper() {
        map = new HashMap<>();
        map.put("Deansgate-Castlefield", "Deansgate Castlefield");
        map.put("Rochdale Interchange","Rochdale Town Centre");
    }

}
