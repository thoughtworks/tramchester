package com.tramchester.integration.testSupport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Optional;

public class ConfigParameterResolver implements ParameterResolver {

    public static final String PARAMETER_KEY = "com.tramchester.config";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(TramchesterConfig.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Optional<String> override = extensionContext.getConfigurationParameter(PARAMETER_KEY);
        if (override.isPresent()) {
            String name = override.get();
            if (name.equals(TramAndTrainGreaterManchesterConfig.class.getSimpleName())) {
                return new TramAndTrainGreaterManchesterConfig();
            }
            else {
                throw new RuntimeException("Unknown test config provided for " + PARAMETER_KEY + " = " + name);
            }
        }
        return new IntegrationTramTestConfig();
    }
}
