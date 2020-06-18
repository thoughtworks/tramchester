package com.tramchester.integration.resources;


import com.tramchester.App;
import com.tramchester.domain.presentation.FeedInfoDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@ExtendWith(DropwizardExtensionsSupport.class)
public class FeedInfoResourceTest {
    public static LocalDate validFrom = LocalDate.of(2020, 6, 18); // year, month, day
    public static LocalDate validUntil = LocalDate.of(2020, 8, 18);

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetFeedinfoCorrectly() {
        String endPoint = "feedinfo";

        Response responce = IntegrationClient.getApiResponse(appExtension, endPoint, Optional.empty(), 200);

        FeedInfoDTO result = responce.readEntity(FeedInfoDTO.class);

        Assertions.assertEquals("Transport for Greater Manchester", result.getPublisherName());
        Assertions.assertEquals("http://www.tfgm.com", result.getPublisherUrl());
        Assertions.assertEquals("Europe/London", result.getTimezone());
        Assertions.assertEquals("en", result.getLang());
        Assertions.assertEquals(validFrom.format(DateTimeFormatter.ofPattern("YYYYMMdd")), result.getVersion());
        Assertions.assertEquals(validFrom, result.validFrom());
        Assertions.assertEquals(validUntil, result.validUntil());
        Assertions.assertFalse(result.getBus());
    }

}
