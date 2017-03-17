package com.tramchester;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.config.AppConfiguration;
import com.tramchester.resources.*;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import org.eclipse.jetty.servlet.FilterHolder;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class App extends Application<AppConfiguration>  {
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

        bootstrap.addBundle(new AssetsBundle("/assets/swagger-ui", "/swagger-ui"));
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) throws Exception {
        dependencies.initialise(configuration);

        environment.lifecycle().addLifeCycleListener(new LifeCycleHandler(dependencies));

        if (configuration.getRedirectHTTP()) {
            RedirectHttpFilter redirectHttpFilter = new RedirectHttpFilter(configuration);
            environment.getApplicationContext().addFilter(new FilterHolder(redirectHttpFilter),
                    "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        environment.jersey().register(dependencies.get(StationResource.class));
        environment.jersey().register(dependencies.get(VersionResource.class));
        environment.jersey().register(dependencies.get(JourneyPlannerResource.class));
        environment.jersey().register(dependencies.get(FeedInfoResource.class));
        environment.jersey().register(dependencies.get(RouteResource.class));
        environment.healthChecks().register("graphDB", dependencies.get(GraphHealthCheck.class));

        filtersForStaticContent(environment);

        // cloudwatch
        MetricRegistry registry = environment.metrics();
        final CloudWatchReporter cloudWatchReporter = CloudWatchReporter.forRegistry(registry,
                dependencies.get(ConfigFromInstanceUserData.class), dependencies.get(SendMetricsToCloudWatch.class));
        cloudWatchReporter.start(1, TimeUnit.MINUTES);

        // swagger ( at /api/swagger.json or /api/swagger.yaml)
        environment.jersey().register(ApiListingResource.class);
        // nulls in the Swagger JSON break SwaggerUI
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // for swagger
        BeanConfig config = new BeanConfig();
        config.setTitle("Tramchester");
        config.setVersion("1.0.0");
        config.setResourcePackage("com.tramchester.resources");
        config.setScan(true);
    }

    private void filtersForStaticContent(Environment environment) {
        int lifeTime = 8 * 24 * 60 * 60;
        StaticAssetFilter filter = new StaticAssetFilter(lifeTime);
        setFilterFor(environment, filter, "javascript", "/javascript/*");
        setFilterFor(environment, filter, "css", "/css/*");
        setFilterFor(environment, filter, "templates", "*.html");
        setFilterFor(environment, filter, "images", "/images/*");
        setFilterFor(environment, filter, "fonts", "/fonts/*");
        setFilterFor(environment, filter, "webfonts", "/webfonts/*");

    }

    private void setFilterFor(Environment environment, StaticAssetFilter filter, String name, String pattern) {
        environment.servlets().addFilter(name, filter).
                addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),true, pattern);
    }


}
