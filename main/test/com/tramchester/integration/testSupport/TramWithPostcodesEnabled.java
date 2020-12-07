package com.tramchester.integration.testSupport;

public class TramWithPostcodesEnabled extends IntegrationTramTestConfig {

    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
