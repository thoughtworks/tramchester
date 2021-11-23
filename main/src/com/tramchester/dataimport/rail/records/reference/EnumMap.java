package com.tramchester.dataimport.rail.records.reference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EnumMap<T extends EnumMap.HasCodes<T>> {
    private final Map<String, T> codes;

    public EnumMap(T[] values) {
        codes = new HashMap<>();
        Arrays.stream(values).forEach(enumValue -> codes.put(enumValue.getCode(), enumValue));
    }

    public void add(T[] values) {
        Arrays.stream(values).forEach(status -> codes.put(status.getCode(), status));
    }

    public T get(char c) {
        return get(String.valueOf(c));
    }

    public T get(String code) {
        return codes.get(code);
    }

    public interface HasCodes<T> {
        String getCode();
    }
}
