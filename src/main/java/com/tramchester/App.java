package com.tramchester;

import com.codahale.metrics.MetricRegistry;
import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.cloud.SignalToCloudformationReady;
import com.tramchester.config.AppConfiguration;
import com.tramchester.healthchecks.DataExpiryHealthCheck;
import com.tramchester.healthchecks.GraphHealthCheck;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.resources.*;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.eclipse.jetty.servlet.FilterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App extends Application<AppConfiguration>  {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static final String SERVICE_NAME = "tramchester";

    private final Dependencies dependencies;
    private ScheduledExecutorService executor;

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

        // WIP new app pages
        bootstrap.addBundle(new AssetsBundle("/app", "/app", "index.html", "app"));

        // api/swagger.json and api/swagger
        bootstrap.addBundle(new SwaggerBundle<AppConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AppConfiguration configuration) {
                return configuration.getSwaggerBundleConfiguration();
            }
        });

        bootstrap.addBundle(new AssetsBundle("/assets/swagger-ui", "/swagger-ui"));
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) throws Exception {
        dependencies.initialise(configuration);

        ScheduledExecutorServiceBuilder builder = environment.lifecycle().scheduledExecutorService("tramchester-%d");
        executor = builder.build();

        environment.lifecycle().addLifeCycleListener(new LifeCycleHandler(dependencies,executor));

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
        environment.jersey().register(dependencies.get(AreaResource.class));
        environment.jersey().register(dependencies.get(DeparturesResource.class));
        environment.healthChecks().register("graphDB", dependencies.get(GraphHealthCheck.class));
        environment.healthChecks().register("dataExpiry", dependencies.get(DataExpiryHealthCheck.class));
        environment.healthChecks().register("liveData", dependencies.get(LiveDataHealthCheck.class));

        filtersForStaticContent(environment);

        // initial load of live data
        LiveDataRepository liveDateRepository = dependencies.get(LiveDataRepository.class);
        liveDateRepository.refreshRespository();

        // cloudwatch
        MetricRegistry registry = environment.metrics();
        final CloudWatchReporter cloudWatchReporter = CloudWatchReporter.forRegistry(registry,
                dependencies.get(ConfigFromInstanceUserData.class), dependencies.get(SendMetricsToCloudWatch.class));
        cloudWatchReporter.start(1, TimeUnit.MINUTES);

        // refresh live data
        executor.scheduleAtFixedRate(() -> {
            try {
                liveDateRepository.refreshRespository();
            } catch (Exception exeception) {
                logger.error("Unable to refresh live data", exeception);
            }
        }, 10,10,TimeUnit.SECONDS);

        // ready to serve traffic
        logger.info("Prepare to signal cloud formation if running in cloud");
        SignalToCloudformationReady signaller = dependencies.get(SignalToCloudformationReady.class);
        signaller.send();

        logger.warn("Now running");
    }

    private void filtersForStaticContent(Environment environment) {
        int lifeTime = 1 * 24 * 60 * 60;
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
