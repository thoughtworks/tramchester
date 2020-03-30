package com.tramchester.deployment;

import software.amazon.awscdk.core.App;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkStack(app, "CdkStack");

        app.synth();
    }
}
