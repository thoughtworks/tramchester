package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.DTO.TramsPositionsDTO;
import com.tramchester.domain.presentation.DTO.TramPositionDTO;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.TramPosition;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.mappers.DeparturesMapper;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;

@Api
@Path("/positions")
@Produces(MediaType.APPLICATION_JSON)
public class TramPositionsResource {

    private final TramPositionInference positionInference;
    private final DeparturesMapper depatureMapper;
    private final ProvidesNow providesNow;

    @Inject
    public TramPositionsResource(TramPositionInference positionInference, DeparturesMapper depatureMapper,
                                 ProvidesNow providesNow) {
        this.positionInference = positionInference;
        this.depatureMapper = depatureMapper;
        this.providesNow = providesNow;
    }

    @GET
    @Timed
    @ApiOperation(value = "Inferred positions of trams within network",
            notes = "Inferred from live tram data feed and timetable data, unfiltered will give all stations whether " +
                    "trams present between them or not",
            response = TramsPositionsDTO.class)
    @CacheControl(maxAge = 10, maxAgeUnit = TimeUnit.SECONDS)
    public Response get(@QueryParam("unfiltered") @DefaultValue("false") String unfilteredRaw) {
        boolean unfilteredFlag = unfilteredRaw.equals("true");

        LocalDate localDate = providesNow.getDate();
        List<TramPosition> results = positionInference.inferWholeNetwork(TramServiceDate.of(localDate),
                providesNow.getNow());
        List<TramPositionDTO> dtoList = results.stream().
                filter(pos -> unfilteredFlag || (!pos.getTrams().isEmpty())).
                map(pos -> new TramPositionDTO(
                        new StationRefWithPosition(pos.getFirst()),
                        new StationRefWithPosition(pos.getSecond()),
                        depatureMapper.mapToDTO(pos.getSecond(), pos.getTrams(), localDate),
                        pos.getCost())).
                collect(Collectors.toList());

        TramsPositionsDTO dto = new TramsPositionsDTO(dtoList);
        return Response.ok(dto).build();
    }

}
