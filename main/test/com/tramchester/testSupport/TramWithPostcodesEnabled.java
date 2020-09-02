package com.tramchester.testSupport;

import com.tramchester.integration.IntegrationTramTestConfig;

public class TramWithPostcodesEnabled extends IntegrationTramTestConfig {

    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
