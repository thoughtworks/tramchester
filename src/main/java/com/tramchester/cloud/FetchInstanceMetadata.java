package com.tramchester.cloud;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class FetchInstanceMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FetchInstanceMetadata.class);
    private static final java.lang.String USER_DATA_PATH = "/latest/user-data";

    private URL instanceDataURL;

    public FetchInstanceMetadata(URL instanceDataRootURL) {

        this.instanceDataURL = instanceDataRootURL;
    }

    public String getUserData() throws MalformedURLException {
        return getDataFrom(new URL(instanceDataURL, USER_DATA_PATH));
    }

    private String getDataFrom(URL url) {
        logger.info("Attempt to get instance user data from " + url.toString());
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url.toString());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            HttpResponse result = httpClient.execute(httpGet);
            HttpEntity entity = result.getEntity();
            entity.writeTo(stream);
        } catch (IOException e) {
            logger.warn("Unable to get instance user data, likely not running in cloud", e);
        }
        return new String(stream.toByteArray());
    }
}
