package com.tramchester.testSupport;

import com.tramchester.integration.IntegrationBusTestConfig;

public class BusWithPostcodesEnabled extends IntegrationBusTestConfig {

    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
