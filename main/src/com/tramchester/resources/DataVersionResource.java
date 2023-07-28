package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.presentation.DTO.DataVersionDTO;
import com.tramchester.repository.ProvidesFeedInfo;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/datainfo")
@Produces(MediaType.APPLICATION_JSON)
public class DataVersionResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(DataVersionResource.class);

    private final ProvidesFeedInfo providesFeedInfo;

    @Inject
    public DataVersionResource(ProvidesFeedInfo providesFeedInfo) {
        this.providesFeedInfo = providesFeedInfo;
    }

    @GET
    @Timed
    @Operation(description = "Information about version of the TFGM data only, Partially Extracted from the feed_info.txt file for a data source")
    @ApiResponse(content = @Content(schema = @Schema(implementation = FeedInfo.class)))
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
