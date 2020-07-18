package com.tramchester.domain;

import java.text.ParseException;
import java.util.Map;

public class EnumParser<T> {

    private final Map<String, T> textMap;

    public EnumParser(Map<String, T> textMap) {

        this.textMap = textMap;
    }

    public T parse(String theText) throws ParseException {
        if (textMap.containsKey(theText)) {
            return textMap.get(theText);
        }
        throw new ParseException("Unexpected transport type %s from " + theText, 0);
    }

}
