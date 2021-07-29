package com.tramchester.testSupport.testTags;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// For marking tests impacted by the closure of eccles line during summer 2021
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Tag("EcclesLineWork")
public @interface EcclesLineWork {
}
