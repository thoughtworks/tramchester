package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.TramsPositionsDTO;
import com.tramchester.domain.presentation.TramPositionDTO;
import com.tramchester.livedata.TramPosition;
import com.tramchester.livedata.TramPositionInference;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/positions")
@Produces(MediaType.APPLICATION_JSON)
public class TramPositionsResource {

    private final TramPositionInference positionInference;

    public TramPositionsResource(TramPositionInference positionInference) {

        this.positionInference = positionInference;
    }

    @GET
    @Timed
    @ApiOperation(value = "Infered positions of trams within network",
            notes = "Infered from live tram data feed and timetime data",
            response = TramsPositionsDTO.class)
    @CacheControl(maxAge = 10, maxAgeUnit = TimeUnit.SECONDS)
    public Response get() {
        List<TramPosition> results = positionInference.inferWholeNetwork();
        List<TramPositionDTO> dtoList = results.stream().
                map(pos -> new TramPositionDTO(new LocationDTO(pos.getFirst()),
                        new LocationDTO(pos.getSecond()),
                        convert(pos.getSecond().getName(), pos.getTrams()))).
                collect(Collectors.toList());
        TramsPositionsDTO dto = new TramsPositionsDTO(dtoList);
        return Response.ok(dto).build();
    }

    private Set<DepartureDTO> convert(String location, Set<DueTram> trams) {
        return trams.stream().map(dueTram -> new DepartureDTO(location, dueTram)).collect(Collectors.toSet());
    }
}
