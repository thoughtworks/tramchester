package com.tramchester.cloud;

import com.tramchester.config.TramchesterConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;

@Singleton
public class FetchInstanceMetadata implements FetchMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FetchInstanceMetadata.class);

    private static final java.lang.String USER_DATA_PATH = "/latest/user-data";

    private final URL instanceDataURL;

    @Inject
    public FetchInstanceMetadata(TramchesterConfig tramchesterConfig) throws MalformedURLException {
        this.instanceDataURL = new URL(tramchesterConfig.getInstanceDataUrl());
    }

    public String getUserData() {
        try {
            URL url = new URL(instanceDataURL, USER_DATA_PATH);
            return getDataFrom(url);
        } catch (MalformedURLException e) {
            logger.warn(format("Unable to fetch instance metadata from %s and %s", instanceDataURL, USER_DATA_PATH),e);
            return "";
        }
    }

    private String getDataFrom(URL url) {
        logger.info("Attempt to getPlatformById instance user data from " + url.toString());
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url.toString());
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000).build();
        httpGet.setConfig(config);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            HttpResponse result = httpClient.execute(httpGet);
            HttpEntity entity = result.getEntity();
            entity.writeTo(stream);
            return stream.toString();
        }
        catch (ConnectTimeoutException timeout) {
            logger.info("Timed out getting meta data, not running in cloud");
            return "";
        }
        catch (IOException e) {
            logger.warn("Unable to getPlatformById instance user data, likely not running in cloud");
            return "";
        }
    }
}
