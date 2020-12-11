package com.tramchester.resources;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.Version;
import com.tramchester.repository.TransportModeRepository;
import com.tramchester.repository.VersionRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Api
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource implements APIResource {

    private final TransportModeRepository repository;

    @Inject
    public VersionResource(TransportModeRepository repository) {
        this.repository = repository;
    }

    @GET
    @ApiOperation(value = "Return version of server code", response = Version.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Version version() {
        return VersionRepository.getVersion();
    }

    @GET
    @ApiOperation(value = "Transport modes enabled on server", response = TransportMode.class, responseContainer = "List")
    @Path("/modes")
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response modes() {
        Set<TransportMode> modes = repository.getModes();

        List<TransportMode> list = new ArrayList<>(modes);

        return Response.ok(list).build();
    }
}
