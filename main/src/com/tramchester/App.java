package com.tramchester;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.tramchester.cloud.*;
import com.tramchester.config.AppConfiguration;
import com.tramchester.healthchecks.*;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.repository.VersionRepository;
import com.tramchester.resources.*;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
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
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


// Great resource for bundles etc here: https://github.com/stve/awesome-dropwizard

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
        logEnvironmentalVars();
        new App().run(args);
    }

    private static void logEnvironmentalVars() {
        Map<String, String> vars = System.getenv();
        vars.forEach((name,value) -> {
            if (("TFGMAPIKEY".equals(name))) {
                value = "****";
            }
            logger.info(String.format("Environment %s=%s", name, value));
        });
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
        // TODO Use configurable assest bundle only for dev env
        bootstrap.addBundle(new ConfiguredAssetsBundle("/app", "/app", "index.html", "app"));

        // api/swagger.json and api/swagger
        bootstrap.addBundle(new SwaggerBundle<AppConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AppConfiguration configuration) {
                SwaggerBundleConfiguration bundleConfiguration = configuration.getSwaggerBundleConfiguration();
                bundleConfiguration.setVersion(VersionRepository.getVersion().getBuildNumber());
                return bundleConfiguration;
            }
        });

        // https://www.tramchester.com/swagger-ui/index.html
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
        environment.healthChecks().register("newData", dependencies.get(NewDataAvailableHealthCheck.class));
        environment.healthChecks().register("liveDataMessages", dependencies.get(LiveDataMessagesHealthCheck.class));


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
        int initialDelay = 10;
        ScheduledFuture<?> liveDataFuture = executor.scheduleAtFixedRate(() -> {
            try {
                liveDateRepository.refreshRespository();
            } catch (Exception exeception) {
                logger.error("Unable to refresh live data", exeception);
            }
        }, initialDelay, configuration.getLiveDataRefreshPeriodSeconds(), TimeUnit.SECONDS);

        // todo into own class
        environment.healthChecks().register("liveDataJobCheck", new HealthCheck() {
            @Override
            protected Result check() {
                if (liveDataFuture.isDone()) {
                    logger.error("Live data job is done");
                    return Result.unhealthy("Live data job is done");
                } else if (liveDataFuture.isCancelled()) {
                    logger.error("Live data job is cancelled");
                    return Result.unhealthy("Live data job is cancelled");
                } else return Result.healthy();
            }
        });

        UploadsLiveData observer = dependencies.get(UploadsLiveData.class);
        liveDateRepository.observeUpdates(observer);

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
