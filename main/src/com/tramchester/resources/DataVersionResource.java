package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.presentation.DTO.DataVersionDTO;
import com.tramchester.repository.ProvidesFeedInfo;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Api
@Path("/datainfo")
@Produces(MediaType.APPLICATION_JSON)
public class DataVersionResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(DataVersionResource.class);

    private final ProvidesFeedInfo providesFeedInfo;

    @Inject
    public DataVersionResource(TramchesterConfig config, ProvidesFeedInfo providesFeedInfo) {
        logger.info("created");
        this.providesFeedInfo = providesFeedInfo;
    }

    @GET
    @Timed
    @ApiOperation(value = "Information about version of the TFGM data only",
            notes = "Partially Extracted from the feed_info.txt file for a data source",
            response = FeedInfo.class)
    @CacheControl(maxAge = 5, maxAgeUnit = TimeUnit.MINUTES)
    public Response get() {
        Map<DataSourceID, FeedInfo> map = providesFeedInfo.getFeedInfos();
        FeedInfo feedinfo = map.get(DataSourceID.tfgm);

        if (feedinfo==null) {
            logger.error("No feedinfo found for tfgm");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        DataVersionDTO dataVersionDTO = new DataVersionDTO(feedinfo);
        return Response.ok(dataVersionDTO).build();
    }
}
