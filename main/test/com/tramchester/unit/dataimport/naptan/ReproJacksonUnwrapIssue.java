package com.tramchester.unit.dataimport.naptan;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/***
 * Jackson support for unwrap of xml root vanished in 2.12 but re-appeared in 2.13
 * causing a bunch of lost time, so add tests here to catch if that happens again in the futrue
 */
public class ReproJacksonUnwrapIssue {

        @JsonRootName("Root")
        static class Root {
            public int id = 1;
        }

        private final XmlMapper xmlMapper = new XmlMapper();

        private final XmlMapper xmlMapperWithIgnoreRoot = XmlMapper.builder()
                .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .build();

        @Test
        public void testReadWithWrapping() throws Exception
        {
            String xml = xmlMapper.writeValueAsString(new Root());
            assertEquals("<Root><id>1</id></Root>", xml);

            String wrapped = "<ignoreMe>"+xml+"</ignoreMe>";
            Root result = xmlMapperWithIgnoreRoot.readValue(wrapped, Root.class);
            assertNotNull(result);
            assertEquals(1, result.id);
        }

}
