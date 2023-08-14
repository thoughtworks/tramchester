package com.tramchester;

import com.codahale.metrics.MetricRegistry;
import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.cloud.SignalToCloudformationReady;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.healthchecks.LiveDataJobHealthCheck;
import com.tramchester.livedata.cloud.CountsUploadedLiveData;
import com.tramchester.livedata.cloud.UploadsLiveData;
import com.tramchester.livedata.tfgm.LiveDataUpdater;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.metrics.RegistersMetricsWithDropwizard;
import com.tramchester.repository.VersionRepository;
import com.tramchester.resources.GraphDatabaseDependencyMarker;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.servlet.FilterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            }));
        try {
            logArgs(args);
            new App().run(args);
        } catch (Exception e) {
            logger.error("Exception, will shutdown ", e);
            //LogManager.shutdown();
            System.exit(-1);
        }
    }

    private static void logArgs(String[] args) {
        final StringBuilder msg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i>0) {
                msg.append(" ");
            }
            msg.append(args[i]);
        }
        logger.info("Args were: " + msg);
    }

    private static void logEnvironmentalVars(List<String> names) {
        final Map<String, String> vars = System.getenv();
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

        logger.info("Add asset bundle for static content");
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
        logger.info("Add asset bundle for swagger-ui");
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
            logger.info("Initial dependency container");
            container.initialise();
            logger.info("Dependency contained is ready");
        }
        catch (Exception exception) {
            logger.error("Uncaught exception during init ", exception);
            throw new RuntimeException("uncaught exception during init", exception);
            //System.exit(-1);
        }

        logger.info("Create schedule executor service");
        final ScheduledExecutorServiceBuilder executorServiceBuilder = environment.lifecycle().scheduledExecutorService("tramchester-%d");
        final ScheduledExecutorService executor = executorServiceBuilder.build();

        logger.info("Add lifecycle event listener");
        environment.lifecycle().addEventListener(new LifeCycleHandler(container, executor));

        final MutableServletContextHandler applicationContext = environment.getApplicationContext();

        logger.info("Add ELB header redirect");
        // Redirect http -> https based on header set by ELB
        final RedirectToHttpsUsingELBProtoHeader redirectHttpFilter = new RedirectToHttpsUsingELBProtoHeader(configuration);
        applicationContext.addFilter(new FilterHolder(redirectHttpFilter), "/*", EnumSet.of(DispatcherType.REQUEST));

        // Redirect / -> /app
        logger.info("Add redirect from / to /app");
        final RedirectToAppFilter redirectToAppFilter = new RedirectToAppFilter();
        applicationContext.addFilter(new FilterHolder(redirectToAppFilter), "/", EnumSet.of(DispatcherType.REQUEST));
        filtersForStaticContent(environment, configuration.getStaticAssetCacheTimeSeconds());

        // api end points registration
        registerAPIResources(environment, configuration.getPlanningEnabled());

        // TODO Check this
        logger.info("Set samesite cookie attribute");
        environment.getApplicationContext().getServletContext().setAttribute(HttpCookie.SAME_SITE_DEFAULT_ATTRIBUTE,
                HttpCookie.SameSite.STRICT);

        // only enable live data present in config
        if (configuration.liveTfgmTramDataEnabled()) {
            logger.info("Start tram live data");
            initLiveDataMetricAndHealthcheck(configuration.getLiveDataConfig(), environment, executor, metricRegistry);
        } else {
            logger.info("Tram live data disabled");
        }

        // report specific metrics to AWS cloudwatch
        if (configuration.getSendCloudWatchMetrics()) {
            logger.info("start cloudwatch metrics");
            final CloudWatchReporter cloudWatchReporter = CloudWatchReporter.forRegistry(metricRegistry,
                    container.get(ConfigFromInstanceUserData.class), container.get(SendMetricsToCloudWatch.class));
            cloudWatchReporter.start(configuration.GetCloudWatchMetricsFrequencyMinutes(), TimeUnit.MINUTES);
        } else {
            logger.warn("Cloudwatch metrics are disabled in config");
        }

        container.registerHealthchecksInto(environment.healthChecks());

        // serve health checks (additionally) on separate URL as we don't want to expose whole of Admin pages
        logger.info("Expose healthchecks at /healthcheck");
        environment.servlets().addServlet(
                "HealthCheckServlet",
                new HealthCheckServlet(environment.healthChecks())
            ).addMapping("/healthcheck");

        // ready to serve traffic
        logger.info("Prepare to signal cloud formation if running in cloud");
        final SignalToCloudformationReady signaller = container.get(SignalToCloudformationReady.class);
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

    private void initLiveDataMetricAndHealthcheck(TfgmTramLiveDataConfig configuration, Environment environment,
                                                  ScheduledExecutorService executor, MetricRegistry metricRegistry) {
        logger.info("Init live data and live data healthchecks");
        // initial load of live data
        final LiveDataUpdater updatesData = container.get(LiveDataUpdater.class);
        updatesData.refreshRespository();

        // refresh live data job
        final int initialDelay = 10;
        final ScheduledFuture<?> liveDataFuture = executor.scheduleAtFixedRate(() -> {
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

        final TramDepartureRepository tramDepartureRepository = container.get(TramDepartureRepository.class);
        final PlatformMessageRepository messageRepository = container.get(PlatformMessageRepository.class);
        final CountsUploadedLiveData countsLiveDataUploads = container.get(CountsUploadedLiveData.class);

        // TODO via DI
        logger.info("Register live data healthchecks");
        final RegistersMetricsWithDropwizard registersMetricsWithDropwizard = new RegistersMetricsWithDropwizard(metricRegistry);
        registersMetricsWithDropwizard.registerMetricsFor(tramDepartureRepository);
        registersMetricsWithDropwizard.registerMetricsFor(messageRepository);
        registersMetricsWithDropwizard.registerMetricsFor(countsLiveDataUploads);

    }

    private void filtersForStaticContent(Environment environment, Integer staticAssetCacheTimeSecond) {
        final StaticAssetFilter filter = new StaticAssetFilter(staticAssetCacheTimeSecond);
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
