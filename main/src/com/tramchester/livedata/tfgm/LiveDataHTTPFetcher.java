package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
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
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

@LazySingleton
public class LiveDataHTTPFetcher implements LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHTTPFetcher.class);
    public static final int CONNECT_TIMEOUT_MS = 30 * 1000;

    private final TfgmTramLiveDataConfig config;

    @Inject
    public LiveDataHTTPFetcher(TramchesterConfig config) {
        this.config = config.getLiveDataConfig();
    }

    @Override
    public String fetch()  {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT_MS).build();
        HttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

        String configLiveDataUrl = config.getDataUrl();
        String liveDataSubscriptionKey = config.getDataSubscriptionKey();
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
                    logger.error(msg);
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
