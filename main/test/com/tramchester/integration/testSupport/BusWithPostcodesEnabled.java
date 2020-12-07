package com.tramchester.integration.testSupport;

public class BusWithPostcodesEnabled extends IntegrationBusTestConfig {

    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
