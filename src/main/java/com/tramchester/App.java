package com.tramchester;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.config.AppConfiguration;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.resources.StationResource;
import com.tramchester.resources.VersionResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class App extends Application<AppConfiguration> {
    public static final String SERVICE_NAME = "tramchester";

    private final Dependencies dependencies;

    public App() {
        this.dependencies = new Dependencies();
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));


        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.htm", "static"));
        bootstrap.addBundle(new AssetsBundle("/assets/css", "/css", null, "css"));
        bootstrap.addBundle(new AssetsBundle("/assets/images", "/images", null, "images"));
        bootstrap.addBundle(new AssetsBundle("/assets/javascript", "/javascript", null, "js"));
        bootstrap.addBundle(new AssetsBundle("/assets/views", "/views", null, "views"));
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) throws Exception {
        dependencies.initialise(configuration);

        if (configuration.isRedirectHTTP()) {
            environment.getApplicationContext().addFilter(RedirectHttpFilter.class,
                    "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        environment.jersey().register(dependencies.get(StationResource.class));
        environment.jersey().register(dependencies.get(VersionResource.class));
        environment.jersey().register(dependencies.get(JourneyPlannerResource.class));
        environment.healthChecks().register("graphDB", dependencies.get(GraphHealthCheck.class));

        MetricRegistry registry = environment.metrics();
        final CloudWatchReporter cloudWatchReporter = CloudWatchReporter.forRegistry(registry,
                dependencies.get(ConfigFromInstanceUserData.class), dependencies.get(SendMetricsToCloudWatch.class));
        cloudWatchReporter.start(1, TimeUnit.MINUTES);
    }

    public void stop() {
        dependencies.close();

    }
}
