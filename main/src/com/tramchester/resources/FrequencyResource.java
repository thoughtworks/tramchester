package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.BoxWithFrequencyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StopCallsForGrid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Path("/frequency")
@Produces(MediaType.APPLICATION_JSON)
public class FrequencyResource extends TransportResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(FrequencyResource.class);

    private final ObjectMapper objectMapper;
    private final DTOFactory DTOFactory;
    private final StopCallsForGrid stopCallsForGrid;

    @Inject
    public FrequencyResource(StopCallsForGrid stopCallsForGrid, ProvidesNow providesNow,
                             DTOFactory DTOFactory) {
        super(providesNow);
        this.DTOFactory = DTOFactory;
        logger.info("created");
        this.objectMapper = JsonMapper.builder().addModule(new AfterburnerModule()).build();
        this.stopCallsForGrid = stopCallsForGrid;
    }

    @GET
    @Timed
    @Operation(description = "Get number of services for the given time period for each grid box")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoxWithServiceFrequency.class)))
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("date") String dateRaw,
                              @QueryParam("startTime") String startTimeRaw,
                              @QueryParam("endTime") String endTimeRaw) {
        logger.info(format("Query for %s gridsize meters, date: '%s' start: '%s' end: '%s", gridSize,
                dateRaw, startTimeRaw, endTimeRaw));

        TramDate date = TramDate.parse(dateRaw);
        TramTime startTime = parseTime(startTimeRaw);
        TramTime endTime = parseTime(endTimeRaw);

        Stream<BoxWithServiceFrequency> results = stopCallsForGrid.getServiceFreqencies(gridSize, date, startTime, endTime);
        Stream<BoxWithFrequencyDTO> dtoStream = results.map(this::createDTO);
        JsonStreamingOutput<BoxWithFrequencyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(dtoStream, objectMapper);

        Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
        return responseBuilder.build();
    }

    private BoxWithFrequencyDTO createDTO(BoxWithServiceFrequency result) {
        List<LocationRefDTO> stopDTOs = result.getStationsWithStopCalls().stream().
                map(DTOFactory::createLocationRefDTO).
                collect(Collectors.toList());

        return new BoxWithFrequencyDTO(result, stopDTOs, result.getNumberOfStopcalls(), new ArrayList<>(result.getModes()));
    }

}
