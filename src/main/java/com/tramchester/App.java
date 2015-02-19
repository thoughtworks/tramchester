package com.tramchester;

import com.tramchester.config.AppConfiguration;
import com.tramchester.resources.StationResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static javax.servlet.DispatcherType.REQUEST;
import static org.eclipse.jetty.servlets.CrossOriginFilter.*;

public class App extends Application<AppConfiguration> {
    public static final String SERVICE_NAME = "tramchester";
    private static final Logger logger = LoggerFactory.getLogger(App.class);

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
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.htm", "static"));
        bootstrap.addBundle(new AssetsBundle("/assets/css", "/css", null, "css"));
        bootstrap.addBundle(new AssetsBundle("/assets/images", "/images", null, "images"));
        bootstrap.addBundle(new AssetsBundle("/assets/javascript", "/javascript", null, "js"));
        bootstrap.addBundle(new AssetsBundle("/assets/views", "/views", null, "views"));
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) throws Exception {
        dependencies.initialise(configuration);
        //Register Resources
        //environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(dependencies.get(StationResource.class));

        addCrossOriginFilter(environment);
    }

    private void addCrossOriginFilter(Environment environment) {
        FilterHolder filterHolder = environment.getApplicationContext().addFilter(CrossOriginFilter.class, "/*", EnumSet.of(REQUEST));
        filterHolder.setInitParameter(EXPOSED_HEADERS_PARAM, "Location");
        filterHolder.setInitParameter(ALLOWED_METHODS_PARAM, "GET,POST,PUT,OPTIONS");
        filterHolder.setInitParameter(ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Access-Control-Request-Headers,cache-control,Access-Control-Allow-Origin,Authorization");
    }


}
