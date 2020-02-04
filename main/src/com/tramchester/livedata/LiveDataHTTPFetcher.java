package com.tramchester.livedata;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

public class LiveDataHTTPFetcher implements LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHTTPFetcher.class);

    private final TramchesterConfig config;

    public LiveDataHTTPFetcher(TramchesterConfig config) {
        this.config = config;
    }

    @Override
    public String fetch()  {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build();
        HttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

        String configLiveDataUrl = config.getLiveDataUrl();
        String liveDataSubscriptionKey = config.getLiveDataSubscriptionKey();
        if (liveDataSubscriptionKey==null) {
            liveDataSubscriptionKey="";
        }

        try {
            if (!liveDataSubscriptionKey.isEmpty()) {
                URIBuilder builder = new URIBuilder(configLiveDataUrl);

                URI uri = builder.build();
                logger.info("Get live tram data from " + uri);
                HttpGet request = new HttpGet(uri);
                request.setHeader("Ocp-Apim-Subscription-Key", liveDataSubscriptionKey);

                HttpResponse response = httpclient.execute(request);
                StatusLine statusLine = response.getStatusLine();
                logger.info(format("Get from %s reponse is %s", uri, statusLine));
                if (statusLine.getStatusCode() != 200) {
                    String msg = format("Unable to getPlatformById from %s response was %s", uri, statusLine);
                    logger.warn(msg);
                    throw new TramchesterException(msg);
                }
                HttpEntity entity = response.getEntity();
                // Note: if not charset in entity then "ISO-8859-1" is used.
                return EntityUtils.toString(entity);

            } else {
                logger.error("No live data API key present in config");
            }
        }
        catch(TramchesterException | IOException | URISyntaxException exception) {
            logger.error("Unable to load live data", exception);
        }
        return "";

    }
}
