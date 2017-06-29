package com.tramchester.integration.resources;

public class RouteCodeToClassMapper {
    private final String prefix = "RouteClass";

    // see tramchester.css

    public String map(String routeId) {
        String code = routeId.substring(4, 8);
        if (code.startsWith("MET")) {
            return mapOldStyle(code);
        } else if (code.startsWith("   ")) {
            return mapNewStyleCode(code);
        }
        return code;
    }

    private String mapNewStyleCode(String code) {
        return prefix + code.trim();
    }

    private String mapOldStyle(String code) {
        switch (code) {
            case "MET1" : return prefix+"D";
            case "MET2" : return prefix+"A";
            case "MET3" : return prefix+"G";
            case "MET4" : return prefix+"E";
            case "MET5" : return prefix+"C";
            case "MET6" : return prefix+"F";
            case "MET7" : return prefix+"H";
            default: return code;
        }
    }
}
