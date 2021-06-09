package com.tramchester.unit.cloud;

import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.FetchMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFromInstanceUserDataTest implements FetchMetadata {

    @Test
    void shouldConvertUserDataToConfig() {
        ConfigFromInstanceUserData provider = new ConfigFromInstanceUserData(this);
        provider.start();
        assertThat(provider.get("WAITURL")).isEqualTo("https://cloudformation-waitcondition-eu-west-1.s3-eu-west-1.amazonaws.com/arn%3Aaws%3Acloudformation%3Aeu-west-1%3A300752856189%3Astack/tramchesterB97UATservers/e5c9d590-1eb2-11e5-8fce-50fa136da090/webDoneWaitHandle?AWSAccessKeyId=AKIAJRBFOG6RPGASDWGA&Expires=1435705359&Signature=csCVnkfYnTeh5qPf0O9mTgiLKAY%3D");
        assertThat(provider.get("ENV")).isEqualTo("UAT");
        assertThat(provider.get("BUILD")).isEqualTo("97");
    }

    @Override
    public String getUserData() {
        return """
                #include
                https://s3-eu-west-1.amazonaws.com/tramchester2dist/97/cloudInit.txt
                https://s3-eu-west-1.amazonaws.com/tramchester2dist/97/setupTramWebServer.sh
                # WAITURL=https://cloudformation-waitcondition-eu-west-1.s3-eu-west-1.amazonaws.com/arn%3Aaws%3Acloudformation%3Aeu-west-1%3A300752856189%3Astack/tramchesterB97UATservers/e5c9d590-1eb2-11e5-8fce-50fa136da090/webDoneWaitHandle?AWSAccessKeyId=AKIAJRBFOG6RPGASDWGA&Expires=1435705359&Signature=csCVnkfYnTeh5qPf0O9mTgiLKAY%3D
                # ENV=UAT
                # ARTIFACTSURL=https://s3-eu-west-1.amazonaws.com/tramchester2dist
                # BUILD=97""";
    }
}
