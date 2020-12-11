package com.tramchester.cloud;

import org.picocontainer.Disposable;
import org.picocontainer.Startable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ConfigFromInstanceUserData implements Startable, Disposable {

    private static final String PREFIX = "#";
    private static final String EQUALS = "=";
    private final FetchMetadata provider;
    private Map<String,String> tokenToValue;

    @Inject
    public ConfigFromInstanceUserData(FetchMetadata provider) {
        this.provider = provider;
    }

    @PostConstruct
    @Override
    public void start() {
        populateMap();
    }

    @Override
    public void stop() {
        // noop
    }

    @PreDestroy
    @Override
    public void dispose() {
        tokenToValue.clear();
    }

    public String get(String token) {
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
