package com.tramchester;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.cloud.SignalToCloudformationReady;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.healthchecks.LiveDataJobHealthCheck;
import com.tramchester.livedata.cloud.CountsUploadedLiveData;
import com.tramchester.livedata.tfgm.LiveDataUpdater;
import com.tramchester.livedata.cloud.UploadsLiveData;
import com.tramchester.livedata.tfgm.DueTramsRepository;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.metrics.RegistersMetricsWithDropwizard;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.repository.VersionRepository;
import com.tramchester.resources.GraphDatabaseDependencyMarker;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.eclipse.jetty.servlet.FilterHolder;
import org.neo4j.logging.shaded.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


// Great resource for bundles etc here: https://github.com/stve/awesome-dropwizard

public class App extends Application<AppConfiguration>  {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final String SERVICE_NAME = "tramchester";

    private GuiceContainerDependencies container;

    public App() {

    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    public static void main(String[] args) {
        logEnvironmentalVars(Arrays.asList("PLACE", "BUILD", "JAVA_OPTS", "BUCKET"));
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                logger.warn("Shutting down");
                LogManager.shutdown();
            }));
        try {
            logArgs(args);
            new App().run(args);
        } catch (Exception e) {
            logger.error("Exception, will shutdown ", e);
            LogManager.shutdown();
            System.exit(-1);
        }
    }

    private static void logArgs(String[] args) {
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i>0) {
                msg.append(" ");
            }
            msg.append(args[i]);
        }
        logger.info("Args were: " + msg);
    }

    private static void logEnvironmentalVars(List<String> names) {
        Map<String, String> vars = System.getenv();
        vars.forEach((name,value) -> {
            if (names.contains(name)) {
                logger.info(format("Environment %s=%s", name, value));
            }
        });
        logger.info("Logged environmental vars");
    }

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {
        logger.info("init bootstrap");

        final SubstitutingSourceProvider substitutingSourceProvider = new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false));

        bootstrap.setConfigurationSourceProvider(substitutingSourceProvider);

        bootstrap.addBundle(new AssetsBundle("/app", "/app", "index.html", "app"));

        // api/swagger.json and api/swagger
        bootstrap.addBundle(new SwaggerBundle<>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AppConfiguration configuration) {
                SwaggerBundleConfiguration bundleConfiguration = configuration.getSwaggerBundleConfiguration();
                bundleConfiguration.setVersion(VersionRepository.getVersion().getBuildNumber());
                return bundleConfiguration;
            }
        });

        // find me at https://www.tramchester.com/api/swagger
        bootstrap.addBundle(new AssetsBundle("/assets/swagger-ui", "/swagger-ui"));
        logger.info("init bootstrap finished");
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) {
        logger.info("App run");

        MetricRegistry metricRegistry = environment.metrics();
        CacheMetrics.RegistersCacheMetrics registersCacheMetrics = new CacheMetrics.DropWizardMetrics(metricRegistry);

        this.container = new ComponentsBuilder().create(configuration, registersCacheMetrics);

        try {
            container.initialise();
        }
        catch (Exception exception) {
            logger.error("Uncaught exception during init ", exception);
            throw new RuntimeException("uncaught excpetion during init", exception);
            //System.exit(-1);
        }

        ScheduledExecutorServiceBuilder executorServiceBuilder = environment.lifecycle().scheduledExecutorService("tramchester-%d");
        ScheduledExecutorService executor = executorServiceBuilder.build();

        environment.lifecycle().addLifeCycleListener(new LifeCycleHandler(container, executor));

        MutableServletContextHandler applicationContext = environment.getApplicationContext();

        // Redirect http -> https based on header set by ELB
        RedirectToHttpsUsingELBProtoHeader redirectHttpFilter = new RedirectToHttpsUsingELBProtoHeader(configuration);
        applicationContext.addFilter(new FilterHolder(redirectHttpFilter), "/*", EnumSet.of(DispatcherType.REQUEST));

        // Redirect / -> /app
        RedirectToAppFilter redirectToAppFilter = new RedirectToAppFilter();
        applicationContext.addFilter(new FilterHolder(redirectToAppFilter), "/", EnumSet.of(DispatcherType.REQUEST));
        filtersForStaticContent(environment, configuration.getStaticAssetCacheTimeSeconds());

        // api end points registration
        registerAPIResources(environment, configuration.getPlanningEnabled());

        // TODO This is the SameSite WORKAROUND, remove once jersey NewCookie adds SameSite method
        environment.jersey().register(new ResponseCookieFilter());

        // WIP - does not work, leave above workaround in place
//        environment.getApplicationContext().getServletContext().setAttribute(HttpCookie.SAME_SITE_DEFAULT_ATTRIBUTE,
//                HttpCookie.SameSite.STRICT);

        // only enable live data present in config
        if (configuration.liveDataEnabled()) {
            initLiveDataMetricAndHealthcheck(configuration.getLiveDataConfig(), environment, executor, metricRegistry);
        }

        // report specific metrics to AWS cloudwatch
        if (configuration.getSendCloudWatchMetrics()) {
            final CloudWatchReporter cloudWatchReporter = CloudWatchReporter.forRegistry(metricRegistry,
                    container.get(ConfigFromInstanceUserData.class), container.get(SendMetricsToCloudWatch.class));
            cloudWatchReporter.start(configuration.GetCloudWatchMetricsFrequencyMinutes(), TimeUnit.MINUTES);
        } else {
            logger.warn("Cloudwatch metrics are disabled in config");
        }

        container.registerHealthchecksInto(environment.healthChecks());

        // serve health checks (additionally) on separate URL as we don't want to expose whole of Admin pages
        environment.servlets().addServlet(
                "HealthCheckServlet",
                new HealthCheckServlet(environment.healthChecks())
            ).addMapping("/healthcheck");

        // ready to serve traffic
        logger.info("Prepare to signal cloud formation if running in cloud");
        SignalToCloudformationReady signaller = container.get(SignalToCloudformationReady.class);
        signaller.send();

        logger.warn("Now running");
    }

    private void registerAPIResources(Environment environment, boolean planningEnabled) {
        logger.info("Registering api endpoints");

        container.getResources().forEach(apiResourceType -> {
            boolean register;
            if (planningEnabled) {
                register = true;
            } else {
                boolean forPlanning = GraphDatabaseDependencyMarker.class.isAssignableFrom(apiResourceType);
                register = !forPlanning;
            }

            final String canonicalName = apiResourceType.getCanonicalName();
            if (register) {
                logger.info("Register " + canonicalName);
                // this injects as a singleton
                Object apiResource = container.get(apiResourceType);
                environment.jersey().register(apiResource);
            } else {
                logger.warn(format("Not registering '%s', planning is disabled", canonicalName));
            }

        });
    }

    private void initLiveDataMetricAndHealthcheck(LiveDataConfig configuration, Environment environment,
                                                  ScheduledExecutorService executor, MetricRegistry metricRegistry) {
        // initial load of live data
        LiveDataUpdater updatesData = container.get(LiveDataUpdater.class);
        updatesData.refreshRespository();

        // refresh live data job
        int initialDelay = 10;
        ScheduledFuture<?> liveDataFuture = executor.scheduleAtFixedRate(() -> {
            try {
                updatesData.refreshRespository();
            } catch (Exception exeception) {
                logger.error("Unable to refresh live data", exeception);
            }
        }, initialDelay, configuration.getRefreshPeriodSeconds(), TimeUnit.SECONDS);
        environment.healthChecks().register("liveDataJobCheck", new LiveDataJobHealthCheck(liveDataFuture));

        // archive live data in S3
        UploadsLiveData observer = container.get(UploadsLiveData.class);
        updatesData.observeUpdates(observer);

        DueTramsRepository dueTramsRepository = container.get(DueTramsRepository.class);
        PlatformMessageRepository messageRepository = container.get(PlatformMessageRepository.class);
        CountsUploadedLiveData countsLiveDataUploads = container.get(CountsUploadedLiveData.class);

        // TODO via DI
        RegistersMetricsWithDropwizard registersMetricsWithDropwizard = new RegistersMetricsWithDropwizard(metricRegistry);
        registersMetricsWithDropwizard.registerMetricsFor(dueTramsRepository);
        registersMetricsWithDropwizard.registerMetricsFor(messageRepository);
        registersMetricsWithDropwizard.registerMetricsFor(countsLiveDataUploads);

    }

    private void filtersForStaticContent(Environment environment, Integer staticAssetCacheTimeSecond) {
        StaticAssetFilter filter = new StaticAssetFilter(staticAssetCacheTimeSecond);
        setFilterFor(environment, filter, "dist", "/app/*");
    }

    private void setFilterFor(Environment environment, StaticAssetFilter filter, String name, String pattern) {
        environment.servlets().addFilter(name, filter).
                addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),true, pattern);
    }

    public GuiceContainerDependencies getDependencies() {
        return container;
    }


}
