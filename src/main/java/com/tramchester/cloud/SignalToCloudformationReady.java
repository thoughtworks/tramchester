package com.tramchester.cloud;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class SignalToCloudformationReady {
    private static final Logger logger = LoggerFactory.getLogger(SignalToCloudformationReady.class);
    private final String url;

    public SignalToCloudformationReady(ConfigFromInstanceUserData providesConfig) {
        url = providesConfig.get("WAITURL");
        if (url!=null) {
            logger.info("Have URL for cloud formation callback " + url);
        }
    }

    public void send() {
        if (url==null) {
            logger.info("Not sending cloud formation callback as URL is not set");
            return;
        }

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post =new HttpPost(url);
        BasicHttpEntity entity = new BasicHttpEntity();
        String content = createContent();
        entity.setContent(new ByteArrayInputStream( content.getBytes() ));
        post.setEntity(entity);
        try {
            HttpResponse response = httpClient.execute(post);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode()!= HttpServletResponse.SC_OK) {
                logger.error("Unexpected status for cloud formation callback " + statusLine.toString());
            }
        } catch (IOException e) {
            logger.error("Erroring sending cloud formation callback to " + url,e);

        }
    }

    private String createContent() {
        UUID uniqueId = UUID.randomUUID();
        String data = "ready to serve traffic";
        return String.format("{\"Status\": \"SUCCESS\", \"Reason\": \"Web Server started\", \"UniqueId\": \"%s\", \"Data\": \"%s\"}",
                uniqueId,
                data);
    }
}
