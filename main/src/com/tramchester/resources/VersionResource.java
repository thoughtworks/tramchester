package com.tramchester.resources;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.presentation.DTO.ConfigDTO;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Api
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {

    private final TransportModeRepository repository;
    private final TramchesterConfig config;

    @Inject
    public VersionResource(TransportModeRepository repository, TramchesterConfig config) {
        this.repository = repository;
        this.config = config;
    }

    @GET
    @ApiOperation(value = "Return version of server code", response = Version.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Version version() {
        return VersionRepository.getVersion();
    }

    @GET
    @ApiOperation(value = "Transport modes enabled on server", response = ConfigDTO.class)
    @Path("/modes")
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response modes() {
        Set<TransportMode> modes = repository.getModes();

        ConfigDTO configDTO = new ConfigDTO(modes, config.hasRemoteDataSourceConfig(DataSourceID.postcode));

        return Response.ok(configDTO).build();
    }
}
