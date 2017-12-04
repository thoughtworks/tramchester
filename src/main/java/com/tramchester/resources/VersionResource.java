package com.tramchester.resources;

import com.tramchester.domain.presentation.Version;
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
    public VersionResource() {
    }

    @GET
    @ApiOperation(value = "Return all routes", response = Version.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Version version() {
        String build = System.getenv("BUILD");
        if (StringUtils.isEmpty(build)) {
            build = "0";
        }
        String version = format("%s.%s", Version.MajorVersion, build);
        return new Version(version);
    }
}
