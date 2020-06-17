package com.tramchester.testSupport;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("dataExpiry")
public @interface DataExpiryCategory {
    // NOTE: this tests fail if data expires within 7 days
}
