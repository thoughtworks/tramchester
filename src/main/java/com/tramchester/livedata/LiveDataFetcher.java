package com.tramchester.livedata;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

public class LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataFetcher.class);

    private final TramchesterConfig config;

    public LiveDataFetcher(TramchesterConfig config) {
        this.config = config;
    }

    public String fetch()  {
        HttpClient httpclient = HttpClients.createDefault();

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
                return EntityUtils.toString(entity);

            } else {
                logger.error("No live data API key present in config");
            }
        }
        catch(TramchesterException exception) {
            logger.error("Unable to load live data",exception);
        } catch (IOException exception) {
            logger.error("Unable to load live data",exception);
        } catch (URISyntaxException exception) {
            logger.error("Unable to load live data",exception);
        }
        return "";


    }
}
