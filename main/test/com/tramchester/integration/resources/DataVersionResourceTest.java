package com.tramchester.integration.resources;


import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.DataVersionDTO;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(DropwizardExtensionsSupport.class)
public class DataVersionResourceTest {

    public static String version = "20210319_16_13_13";

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetDataVersionCorrectly() {
        String endPoint = "datainfo";

        Response responce = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, responce.getStatus());

        DataVersionDTO result = responce.readEntity(DataVersionDTO.class);

        assertEquals(version, result.getVersion());

    }

}
