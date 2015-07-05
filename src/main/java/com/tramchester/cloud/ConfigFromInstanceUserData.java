package com.tramchester.cloud;

import java.util.HashMap;
import java.util.Map;

public class ConfigFromInstanceUserData {

    public static final String PREFIX = "#";
    public static final String EQUALS = "=";
    private FetchMetadata provider;
    private Map<String,String> tokenToValue;

    public ConfigFromInstanceUserData(FetchMetadata provider) {

        this.provider = provider;
    }

    public String get(String token) {
        populateMap();
        return tokenToValue.get(token);
    }

    private void populateMap() {
        if (tokenToValue!=null) {
            return;
        }
        tokenToValue = new HashMap<>();
        String userData = provider.getUserData();
        String[] lines = userData.split("\n");
        for (String line : lines) {
            if (line.startsWith(PREFIX)) {
                String[] components = line.substring(1).split(EQUALS,2);
                if (components.length==2) {
                    String key = components[0].trim();
                    tokenToValue.put(key, components[1]);
                }
            }
        }
    }
}
