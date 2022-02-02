package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.livedata.TramPosition;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.livedata.domain.DTO.TramPositionDTO;
import com.tramchester.livedata.domain.DTO.TramsPositionsDTO;
import com.tramchester.livedata.mappers.DeparturesMapper;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/positions")
@Produces(MediaType.APPLICATION_JSON)
public class TramPositionsResource implements APIResource, JourneyPlanningMarker {
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
    @ApiOperation(value = "Inferred positions of trams within network",
            notes = "Inferred from live tram data feed and timetable data, unfiltered will give all stations whether " +
                    "trams present between them or not",
            response = TramsPositionsDTO.class)
    @CacheControl(maxAge = 10, maxAgeUnit = TimeUnit.SECONDS)
    public Response get(@QueryParam("unfiltered") @DefaultValue("false") String unfilteredRaw) {
        logger.info("Get tram positions unfiltered="+unfilteredRaw);

        boolean unfilteredFlag = unfilteredRaw.equals("true");

        LocalDate localDate = providesNow.getDate();
        List<TramPosition> results = positionInference.inferWholeNetwork(TramServiceDate.of(localDate), providesNow.getNow());
        List<TramPositionDTO> dtoList = results.stream().
                filter(pos -> unfilteredFlag || (!pos.getTrams().isEmpty())).
                map(pos -> new TramPositionDTO(
                        DTOFactory.createLocationRefWithPosition(pos.getFirst()),
                        DTOFactory.createLocationRefWithPosition(pos.getSecond()),
                        depatureMapper.mapToDTO(pos.getSecond(), pos.getTrams(), localDate),
                        pos.getCost())).
                collect(Collectors.toList());

        TramsPositionsDTO dto = new TramsPositionsDTO(dtoList);
        return Response.ok(dto).build();
    }

}
