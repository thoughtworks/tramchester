package com.tramchester.unit.cloud.data;

import com.tramchester.cloud.data.S3Keys;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class s3KeysTest {
    private  S3Keys s3Keys;

    @BeforeEach
    void beforeEachTest() {
        TramchesterConfig config = TestEnv.GET();
        s3Keys = new S3Keys(config);
    }

    @Test
    void shouldCreateExpectedPrefix() {
        LocalDate date = LocalDate.of(2020,11,29);
        String result = s3Keys.createPrefix(date);

        assertEquals("test/20201129", result);
    }

    @Test
    void shouldCreatedExpectedKey() {

        LocalDateTime date = LocalDateTime.of(2020,11,29, 15,42,55);
        String result = s3Keys.create(date);

        assertEquals("test/20201129/15:42:55", result);
    }

    @Test
    void shouldParseKey() throws S3Keys.S3KeyException {
        LocalDateTime expected = LocalDateTime.of(2020,11,29, 15,42,55);
        String key = "test/20201129/15:42:55";

        LocalDateTime result = s3Keys.parse(key);

        assertEquals(expected, result);
    }

    @Test
    void shouldThrowOnBadDate() {
        Assertions.assertThrows(S3Keys.S3KeyException.class, () -> {
            s3Keys.parse("test/2020xx29/15:42:55");
        });
    }

    @Test
    void shouldThrowOnBadTime() {
        Assertions.assertThrows(S3Keys.S3KeyException.class, () -> {
            s3Keys.parse("test/20201129/15:xx:55");
        });
    }

    @Test
    void shouldThrowOnBadStructureExtra() {
        Assertions.assertThrows(S3Keys.S3KeyException.class, () -> {
            s3Keys.parse("test/20201129/15:xx:55/");
        });
    }

    @Test
    void shouldThrowOnBadStructureMissing() {
        Assertions.assertThrows(S3Keys.S3KeyException.class, () -> {
            s3Keys.parse("test/");
        });
    }
}
