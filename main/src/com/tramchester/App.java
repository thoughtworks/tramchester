package com.tramchester;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.tramchester.cloud.*;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.healthchecks.LiveDataJobHealthCheck;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.repository.VersionRepository;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jetty.MutableServletContextHandler;
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

    private static final String SERVICE_NAME = "tramchester";

    private final Dependencies dependencies;

    public App() {
        this.dependencies = new Dependencies();
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    public static void main(String[] args) throws Exception {
        logEnvironmentalVars();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.warn("Shutting down")));
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
        logger.info("Logged environmental vars");
    }

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {
        logger.info("initialize");
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));

        // TODO Use configurable assest bundle only for dev env?
        bootstrap.addBundle(new ConfiguredAssetsBundle("/app", "/app", "index.html", "app"));

        // api/swagger.json and api/swagger
        bootstrap.addBundle(new SwaggerBundle<>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AppConfiguration configuration) {
                SwaggerBundleConfiguration bundleConfiguration = configuration.getSwaggerBundleConfiguration();
                bundleConfiguration.setVersion(VersionRepository.getVersion().getBuildNumber());
                return bundleConfiguration;
            }
        });

        // https://www.tramchester.com/api/swagger
        bootstrap.addBundle(new AssetsBundle("/assets/swagger-ui", "/swagger-ui"));
        logger.info("initialize finished");
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) {
        logger.info("App run");

        try {
            dependencies.initialise(configuration);
        }
        catch (Exception exception) {
            logger.error("Uncaught exception during init ", exception);
            System.exit(-1);
        }

        ScheduledExecutorServiceBuilder executorServiceBuilder = environment.lifecycle().scheduledExecutorService("tramchester-%d");
        ScheduledExecutorService executor = executorServiceBuilder.build();

        environment.lifecycle().addLifeCycleListener(new LifeCycleHandler(dependencies, executor));

        MutableServletContextHandler applicationContext = environment.getApplicationContext();

        // Redirect http -> https based on header set by ELB
        RedirectToHttpsUsingELBProtoHeader redirectHttpFilter = new RedirectToHttpsUsingELBProtoHeader(configuration);
        applicationContext.addFilter(new FilterHolder(redirectHttpFilter),
                "/*", EnumSet.of(DispatcherType.REQUEST));

        // Redirect / -> /app
        RedirectToAppFilter redirectToAppFilter = new RedirectToAppFilter();
        applicationContext.addFilter(new FilterHolder(redirectToAppFilter),
                "/", EnumSet.of(DispatcherType.REQUEST));
        filtersForStaticContent(environment);

        // api end points registration
        dependencies.getResources().forEach(apiResource -> environment.jersey().register(apiResource));
        // TOOD This is the SameSite WORKAROUND, remove once jersey NewCookie adds SameSite method
        environment.jersey().register(new ResponseCookieFilter());

        MetricRegistry metricRegistry = environment.metrics();

        // only enable live data if tram's enabled

        if (configuration.getTransportModes().contains(GTFSTransportationType.tram)) {
            // initial load of live data
            LiveDataRepository liveDataRepository = dependencies.get(LiveDataRepository.class);
            liveDataRepository.refreshRespository();
            // custom metrics for live data and messages
            metricRegistry.register(MetricRegistry.name(LiveDataRepository.class, "liveData", "number"),
                    (Gauge<Integer>) liveDataRepository::upToDateEntries);
            metricRegistry.register(MetricRegistry.name(LiveDataRepository.class, "liveData", "messages"),
                    (Gauge<Integer>) liveDataRepository::entriesWithMessages);

            // refresh live data
            int initialDelay = 10;
            ScheduledFuture<?> liveDataFuture = executor.scheduleAtFixedRate(() -> {
                try {
                    liveDataRepository.refreshRespository();
                } catch (Exception exeception) {
                    logger.error("Unable to refresh live data", exeception);
                }
            }, initialDelay, configuration.getLiveDataRefreshPeriodSeconds(), TimeUnit.SECONDS);

            // archive live data in S3
            UploadsLiveData observer = dependencies.get(UploadsLiveData.class);
            liveDataRepository.observeUpdates(observer);

            environment.healthChecks().register("liveDataJobCheck", new LiveDataJobHealthCheck(liveDataFuture));
        }

        CacheMetricSet cacheMetrics = new CacheMetricSet(dependencies.getHasCacheStat(), metricRegistry);
        cacheMetrics.prepare();

        // report specific metrics to AWS cloudwatch
        final CloudWatchReporter cloudWatchReporter = CloudWatchReporter.forRegistry(metricRegistry,
                dependencies.get(ConfigFromInstanceUserData.class), dependencies.get(SendMetricsToCloudWatch.class));
        cloudWatchReporter.start(1, TimeUnit.MINUTES);

        // health check registration
        dependencies.getHealthChecks().forEach(healthCheck ->
                environment.healthChecks().register(healthCheck.getName(), healthCheck));

        // serve health checks (additionally) on separate URL as we don't want to expose whole of Admin pages
        environment.servlets().addServlet(
                "HealthCheckServlet",
                new HealthCheckServlet(environment.healthChecks())
            ).addMapping("/healthcheck");

        // ready to serve traffic
        logger.info("Prepare to signal cloud formation if running in cloud");
        SignalToCloudformationReady signaller = dependencies.get(SignalToCloudformationReady.class);
        signaller.send();

        logger.warn("Now running");
    }

    private void filtersForStaticContent(Environment environment) {
        int lifeTime = 5 * 60; // 5 minutes
        StaticAssetFilter filter = new StaticAssetFilter(lifeTime);
        setFilterFor(environment, filter, "dist", "/app/dist/*");
        setFilterFor(environment, filter, "html", "/app/index.html");
    }

    private void setFilterFor(Environment environment, StaticAssetFilter filter, String name, String pattern) {
        environment.servlets().addFilter(name, filter).
                addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),true, pattern);
    }


}
