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
import java.time.format.DateTimeFormatter;


@ExtendWith(DropwizardExtensionsSupport.class)
public class DataVersionResourceTest {
    public static LocalDate validFrom = LocalDate.of(2020, 12, 22); // year, month, day
    public static LocalDate validUntil = LocalDate.of(2021, 2, 22);

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetDataVersionCorrectly() {
        String endPoint = "datainfo";

        Response responce = IntegrationClient.getApiResponse(appExtension, endPoint);
        Assertions.assertEquals(200, responce.getStatus());

        DataVersionDTO result = responce.readEntity(DataVersionDTO.class);

        Assertions.assertEquals(validFrom.format(DateTimeFormatter.ofPattern("YYYYMMdd")), result.getVersion());
        Assertions.assertEquals(validFrom, result.validFrom());
        Assertions.assertEquals(validUntil, result.validUntil());
    }

}
