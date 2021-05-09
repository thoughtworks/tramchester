package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.repository.postcodes.PostcodeRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/postcodes")
@Produces(MediaType.APPLICATION_JSON)
public class PostcodeResource {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeResource.class);

    private final PostcodeRepository postcodeRepository;
    private final PostcodeDataImporter importer;

    @Inject
    public PostcodeResource(PostcodeRepository postcodeRepository, PostcodeDataImporter importer) {
        this.postcodeRepository = postcodeRepository;
        this.importer = importer;
    }

    @GET
    @Timed
    @ApiOperation(value = "Return all loaded (local) postcodes", response = PostcodeDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS, isPrivate = false)
    public Response getAll(@Context Request request) {
        logger.info("Get all postcodes");

        // TODO this could potentilly be very big.....
        Collection<PostcodeLocation> allPostcodes = postcodeRepository.getPostcodes();

        LocalDateTime modTime = importer.getTargetFolderModTime();
        Date date = Date.from(modTime.toInstant(ZoneOffset.UTC));

        Response.ResponseBuilder builder = request.evaluatePreconditions(date);

        if (builder==null) {
            logger.debug("modified");
            List<PostcodeDTO> postcodeDTOs = mapToDTO(allPostcodes);
            return Response.ok(postcodeDTOs).lastModified(date).build();
        } else {
            logger.debug("Not modified");
            return builder.build();
        }
        
    }

    private List<PostcodeDTO> mapToDTO(Collection<PostcodeLocation> allPostcodes) {
        return allPostcodes.stream().map(PostcodeDTO::new).collect(Collectors.toList());
    }
}
