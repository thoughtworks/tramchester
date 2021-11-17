package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RouteDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(RouteDataLoader.class);

    private final WriteableTransportData buildable;
    private final GTFSSourceConfig sourceConfig;
    private final TransportEntityFactory factory;

    public RouteDataLoader(WriteableTransportData buildable, GTFSSourceConfig sourceConfig, TransportEntityFactory factory) {
        this.buildable = buildable;
        this.sourceConfig = sourceConfig;
        this.factory = factory;
    }

    public ExcludedRoutes load(Stream<RouteData> routeDataStream, CompositeIdMap<Agency, MutableAgency> allAgencies) {
        Set<GTFSTransportationType> transportModes = sourceConfig.getTransportGTFSModes();
        AtomicInteger count = new AtomicInteger();

        ExcludedRoutes excludedRoutes = new ExcludedRoutes();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        routeDataStream.forEach(routeData -> {
            IdFor<Agency> agencyId = routeData.getAgencyId();
            boolean missingAgency = !allAgencies.hasId(agencyId);
            if (missingAgency) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = factory.getRouteType(routeData, agencyId);

            if (transportModes.contains(routeType)) {
                DataSourceID dataSourceID = sourceConfig.getDataSourceId();
                MutableAgency agency = missingAgency ? createMissingAgency(dataSourceID, allAgencies, agencyId, factory)
                        : allAgencies.get(agencyId);

                MutableRoute route = factory.createRoute(routeType, routeData, agency);

                agency.addRoute(route);
                if (!buildable.hasAgencyId(agencyId)) {
                    buildable.addAgency(agency);
                }
                buildable.addRoute(route);

                count.getAndIncrement();

            } else {
                IdFor<Route> routeId = routeData.getId();
                excludedRoutes.excludeRoute(factory.createRouteId(routeId));
            }
        });
        excludedRoutes.recordInLog(transportModes);
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.numOfExcluded());
        return excludedRoutes;
    }

    private MutableAgency createMissingAgency(DataSourceID dataSourceID, CompositeIdMap<Agency, MutableAgency> allAgencies, IdFor<Agency> agencyId,
                                              TransportEntityFactory factory) {
        MutableAgency unknown = factory.createUnknownAgency(dataSourceID, agencyId);
        logger.error("Created agency" + unknown + " for " + dataSourceID);
        allAgencies.add(unknown);
        return unknown;
    }

    public static class ExcludedRoutes {
        private final IdSet<Route> excludedRouteIds;

        private ExcludedRoutes() {
            excludedRouteIds = new IdSet<>();
        }

        public void excludeRoute(IdFor<Route> routeId) {
            excludedRouteIds.add(routeId);
        }

        public boolean wasExcluded(IdFor<Route> routeId) {
            return excludedRouteIds.contains(routeId);
        }

        public IdSet<Route> getExcluded() {
            return excludedRouteIds;
        }

        public int numOfExcluded() {
            return excludedRouteIds.size();
        }

        public void clear() {
            excludedRouteIds.clear();
        }

        public void recordInLog(Set<GTFSTransportationType> transportModes) {
            if (excludedRouteIds.isEmpty()) {
                return;
            }
            logger.info("Excluded the following route id's as did not match modes " + transportModes + " routes: " + excludedRouteIds);
        }
    }
}
