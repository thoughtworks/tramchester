package com.tramchester.resources;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(servers = {@Server(url = "/api") },
    info = @Info(title = "tramchester", termsOfService = "Use with permission only"))
public interface APIResource {
    // marker
}
