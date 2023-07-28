package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.domain.DTO.TramPositionDTO;
import com.tramchester.livedata.domain.DTO.TramsPositionsDTO;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.livedata.tfgm.TramPosition;
import com.tramchester.livedata.tfgm.TramPositionInference;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/positions")
@Produces(MediaType.APPLICATION_JSON)
public class TramPositionsResource implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(TramPositionsResource.class);

    private final TramPositionInference positionInference;
    private final DeparturesMapper depatureMapper;
    private final DTOFactory DTOFactory;
    private final ProvidesNow providesNow;

    @Inject
    public TramPositionsResource(TramPositionInference positionInference, DeparturesMapper depatureMapper,
                                 DTOFactory DTOFactory, ProvidesNow providesNow) {
        this.DTOFactory = DTOFactory;
        logger.info("created");
        this.positionInference = positionInference;
        this.depatureMapper = depatureMapper;
        this.providesNow = providesNow;
    }

    @GET
    @Timed
    @Operation(description = "Inferred positions of trams within network." +
            "Inferred from live tram data feed and timetable data, unfiltered will give all stations whether " +
                    "trams present between them or not")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = TramsPositionsDTO.class))))
    @CacheControl(maxAge = 10, maxAgeUnit = TimeUnit.SECONDS)
    public Response get(@QueryParam("unfiltered") @DefaultValue("false") String unfilteredRaw) {
        logger.info("Get tram positions unfiltered="+unfilteredRaw);

        boolean unfilteredFlag = unfilteredRaw.equals("true");

        LocalDateTime localDateTime = providesNow.getDateTime();
        List<TramPosition> results = positionInference.inferWholeNetwork(localDateTime);
        List<TramPositionDTO> dtoList = results.stream().
                filter(pos -> unfilteredFlag || (!pos.getTrams().isEmpty())).
                map(pos -> new TramPositionDTO(
                        DTOFactory.createLocationRefWithPosition(pos.getFirst()),
                        DTOFactory.createLocationRefWithPosition(pos.getSecond()),
                        depatureMapper.mapToDTO(pos.getTrams(), localDateTime),
                        pos.getCost())).
                collect(Collectors.toList());

        TramsPositionsDTO dto = new TramsPositionsDTO(dtoList);
        return Response.ok(dto).build();
    }

}
