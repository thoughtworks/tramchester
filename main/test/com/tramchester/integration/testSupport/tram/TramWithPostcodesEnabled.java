package com.tramchester.integration.testSupport.tram;

public class TramWithPostcodesEnabled extends IntegrationTramTestConfig {
    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
