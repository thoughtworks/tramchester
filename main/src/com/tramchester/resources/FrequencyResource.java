package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.presentation.DTO.BoxWithFrequencyDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StopCallsForGrid;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Api
@Path("/frequency")
@Produces(MediaType.APPLICATION_JSON)
public class FrequencyResource extends TransportResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(FrequencyResource.class);

    private final ObjectMapper objectMapper;
    private final StopCallsForGrid stopCallsForGrid;

    @Inject
    public FrequencyResource(ObjectMapper objectMapper, StopCallsForGrid stopCallsForGrid, ProvidesNow providesNow) {
        super(providesNow);
        logger.info("created");
        this.objectMapper = objectMapper;
        this.stopCallsForGrid = stopCallsForGrid;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get number of services for the given time period for each grid box", response = BoxWithServiceFrequency.class)
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("date") String dateRaw,
                              @QueryParam("startTime") String startTimeRaw,
                              @QueryParam("endTime") String endTimeRaw) {
        logger.info(format("Query for %s gridsize meters, date: '%s' start: '%s' end: '%s", gridSize,
                dateRaw, startTimeRaw, endTimeRaw));

        LocalDate date = LocalDate.parse(dateRaw);
        TramTime startTime = parseTime(startTimeRaw);
        TramTime endTime = parseTime(endTimeRaw);

        Set<BoxWithServiceFrequency> results = stopCallsForGrid.getServiceFreqencies(gridSize, date, startTime, endTime);
        Stream<BoxWithFrequencyDTO> dtoStream = results.stream().map(this::createDTO);
        JsonStreamingOutput<BoxWithFrequencyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(dtoStream, objectMapper);

        Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
        return responseBuilder.build();
    }

    private BoxWithFrequencyDTO createDTO(BoxWithServiceFrequency result) {
        List<StationRefDTO> stopDTOs = result.getStationsWithStopCalls().stream().map(StationRefDTO::new).collect(Collectors.toList());
        return new BoxWithFrequencyDTO(result, stopDTOs, result.getNumberOfStopcalls());
    }

}
