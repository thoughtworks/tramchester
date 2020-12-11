package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.presentation.Version;
import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;

@LazySingleton
public class VersionRepository {

    public static Version getVersion() {
        String build = System.getenv("BUILD");
        if (StringUtils.isEmpty(build)) {
            build = "0";
        }
        String version = format("%s.%s", Version.MajorVersion, build);
        return new Version(version);
    }
}
