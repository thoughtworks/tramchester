package com.tramchester.resources;

import com.tramchester.domain.presentation.Version;
import com.tramchester.repository.VersionRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Api
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {
    private final VersionRepository versionRepository;

    public VersionResource(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @GET
    @ApiOperation(value = "Return version of server code", response = Version.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Version version() {
        return versionRepository.getVersion();
    }
}
