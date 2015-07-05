package com.tramchester.cloud;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class TestSignalToCloudFormationReady {

    // examplePostData = "{\"Status\": \"SUCCESS\", \"Reason\": \"Web Server started\", \"UniqueId\": \"UNIQUEID\", \"Data\": \"887\"}";

    @Test
    public void shouldSignalWhenUserDataContainsCallbackURL() throws Exception {

        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "# WAITURL=http://localhost:8080/callbackURL");
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPostedData();
        assertThat(sentData).contains("\"Status\": \"SUCCESS\"");
        assertThat(sentData).contains("\"Reason\": \"Web Server started\"");
    }

    @Test
    public void shouldNotSignalWhenUserDataDoesNotContainCallbackURL() throws Exception {
        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "# NOTHERE=http://localhost:8080/callbackURL");
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPostedData();
        assertThat(sentData).isNull();
    }

    @Test
    public void shouldNotSignalWhenNoUserDataAvailable() throws Exception {
        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "");
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPostedData();
        assertThat(sentData).isNull();

    }
}
