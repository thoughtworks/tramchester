package com.tramchester.testSupport;

import com.tramchester.integration.IntegrationTramTestConfig;

public class WithPostcodesEnabled extends IntegrationTramTestConfig {

    @Override
    public boolean getLoadPostcodes() {
        return true;
    }
}
