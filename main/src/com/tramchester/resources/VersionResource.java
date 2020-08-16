package com.tramchester.resources;

import com.tramchester.domain.presentation.Version;
import com.tramchester.repository.VersionRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.TimeUnit;

@Api
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource implements APIResource {

    public VersionResource() {
    }

    @GET
    @ApiOperation(value = "Return version of server code", response = Version.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Version version() {
        return VersionRepository.getVersion();
    }
}
