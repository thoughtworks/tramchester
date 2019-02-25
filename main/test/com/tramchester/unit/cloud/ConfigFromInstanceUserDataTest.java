package com.tramchester.unit.cloud;

import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.FetchMetadata;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ConfigFromInstanceUserDataTest implements FetchMetadata {

    private String exampleMetaData = "#include\n" +
            "https://s3-eu-west-1.amazonaws.com/tramchester2dist/97/cloudInit.txt\n" +
            "https://s3-eu-west-1.amazonaws.com/tramchester2dist/97/setupTramWebServer.sh\n" +
            "# WAITURL=https://cloudformation-waitcondition-eu-west-1.s3-eu-west-1.amazonaws.com/arn%3Aaws%3Acloudformation%3Aeu-west-1%3A300752856189%3Astack/tramchesterB97UATservers/e5c9d590-1eb2-11e5-8fce-50fa136da090/webDoneWaitHandle?AWSAccessKeyId=AKIAJRBFOG6RPGASDWGA&Expires=1435705359&Signature=csCVnkfYnTeh5qPf0O9mTgiLKAY%3D\n" +
            "# ENV=UAT\n" +
            "# ARTIFACTSURL=https://s3-eu-west-1.amazonaws.com/tramchester2dist\n" +
            "# BUILD=97";

    @Test
    public void shouldConvertUserDataToConfig() {
        ConfigFromInstanceUserData provider = new ConfigFromInstanceUserData(this);
        assertThat(provider.get("WAITURL")).isEqualTo("https://cloudformation-waitcondition-eu-west-1.s3-eu-west-1.amazonaws.com/arn%3Aaws%3Acloudformation%3Aeu-west-1%3A300752856189%3Astack/tramchesterB97UATservers/e5c9d590-1eb2-11e5-8fce-50fa136da090/webDoneWaitHandle?AWSAccessKeyId=AKIAJRBFOG6RPGASDWGA&Expires=1435705359&Signature=csCVnkfYnTeh5qPf0O9mTgiLKAY%3D");
        assertThat(provider.get("ENV")).isEqualTo("UAT");
        assertThat(provider.get("BUILD")).isEqualTo("97");
    }

    @Override
    public String getUserData() {
        return exampleMetaData;
    }
}
