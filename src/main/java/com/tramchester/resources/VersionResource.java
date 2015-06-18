package com.tramchester.resources;

import com.tramchester.domain.Version;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {
    public VersionResource() {
    }

    @GET
    public Version version() {
        String build = System.getenv("BUILD");
        if (StringUtils.isEmpty(build)) {
            build = "0";
        }
        return new Version(build);
    }


}
