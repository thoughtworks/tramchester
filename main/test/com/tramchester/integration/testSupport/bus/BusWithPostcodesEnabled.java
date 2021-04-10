package com.tramchester.integration.testSupport.bus;

public class BusWithPostcodesEnabled extends IntegrationBusTestConfig {
    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
