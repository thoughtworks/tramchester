package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.repository.PostcodeRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/postcodes")
@Produces(MediaType.APPLICATION_JSON)
public class PostcodeResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeResource.class);

    private final PostcodeRepository postcodeRepository;

    public PostcodeResource(PostcodeRepository postcodeRepository) {
        this.postcodeRepository = postcodeRepository;
    }

    @GET
    @Timed
    @ApiOperation(value = "Return all loaded (local) postcodes", response = PostcodeDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response getAll() {
        logger.info("Get all postcodes");

        Collection<PostcodeLocation> allPostcodes = postcodeRepository.getPostcodes();

        List<PostcodeDTO> postcodeDTOs = mapToDTO(allPostcodes);
        return Response.ok(postcodeDTOs).build();

    }

    private List<PostcodeDTO> mapToDTO(Collection<PostcodeLocation> allPostcodes) {
        return allPostcodes.stream().map(PostcodeDTO::new).collect(Collectors.toList());
    }
}
