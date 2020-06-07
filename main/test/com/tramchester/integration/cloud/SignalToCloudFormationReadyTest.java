package com.tramchester.integration.cloud;

import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SignalToCloudformationReady;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class SignalToCloudFormationReadyTest {

    @Test
    void shouldSignalWhenUserDataContainsCallbackURL() throws Exception {

        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "# WAITURL=http://localhost:8080/callbackURL");
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPutData();
        assertThat(sentData).contains("\"Status\": \"SUCCESS\"");
        assertThat(sentData).contains("\"Reason\": \"Web Server started\"");

        assertThat(stubbedServer.getContentHeader()).isEmpty(); // aws requires this header is empty or not set
    }

    @Test
    void shouldNotSignalWhenUserDataDoesNotContainCallbackURL() throws Exception {
        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "# NOTHERE=http://localhost:8080/callbackURL");
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPutData();
        assertThat(sentData).isNull();
    }

    @Test
    void shouldNotSignalWhenNoUserDataAvailable() throws Exception {
        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "");
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPutData();
        assertThat(sentData).isNull();

    }
}
