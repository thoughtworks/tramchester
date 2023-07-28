package com.tramchester.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@LazySingleton
public class SignalToCloudformationReady {
    private static final Logger logger = LoggerFactory.getLogger(SignalToCloudformationReady.class);
    private final String url;

    @Inject
    public SignalToCloudformationReady(ConfigFromInstanceUserData providesConfig) {
        url = providesConfig.get("WAITURL");
        if (url!=null) {
            logger.info("Have URL for cloud formation triggered " + url);
        }
    }

    public void send() {
        if (url==null) {
            logger.info("Not sending cloud formation triggered as URL is not set");
            return;
        }

        logger.info("Attempt to send PUT to cloud formation to signal code ready " + url);
        HttpClient httpClient = HttpClients.createDefault();
        HttpPut put =new HttpPut(url);
        put.setEntity(createEntity());

        put.setHeader("Content-Type", ""); // aws docs say empty content header is required

        try {
            HttpResponse response = httpClient.execute(put);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode()!= HttpServletResponse.SC_OK) {
                logger.error("Unexpected status for cloud formation triggered " + statusLine);
            } else {
                logger.info("cloud formation POST made OK");
            }
        } catch (IOException e) {
            logger.error("Erroring sending cloud formation triggered to " + url,e);
        }
    }

    private HttpEntity createEntity() {
        String content = createContent();
        logger.info("Sending data " + content);
        return new ByteArrayEntity(content.getBytes());
    }

    private String createContent() {
        UUID uniqueId = UUID.randomUUID();
        String data = "ready to serve traffic";
        return String.format("{\"Status\": \"SUCCESS\", \"Reason\": \"Web Server started\", \"UniqueId\": \"%s\", \"Data\": \"%s\"}",
                uniqueId,
                data);
    }
}
