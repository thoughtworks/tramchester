package com.tramchester.graph.search;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

public class ServiceNodeCache extends PopulateNodeIdsFromQuery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNodeCache.class);

    private final ServiceRepository serviceRepository;
    private final Map<IdFor<Service>, Set<Long>> idsForEachService;

    @Inject
    public ServiceNodeCache(GraphDatabase graphDatabaseService, ServiceRepository serviceRepository, StagedTransportGraphBuilder.Ready ready) {
        super(graphDatabaseService);
        this.serviceRepository = serviceRepository;
        idsForEachService = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");

        // TODO Very slow building this for trains etc so need to cache to disc

        IdSet<Service> servicesIds = serviceRepository.getServices().stream().collect(IdSet.collector());

        servicesIds.stream().parallel().forEach(serviceId -> {
            Set <Long> relationshipsFor = getRelationshipsFor(serviceId);
            idsForEachService.put(serviceId, relationshipsFor);
            logger.info("Added " + relationshipsFor.size() + " nodes for service " + serviceId);
        });

        Optional<Integer> totalAdded = idsForEachService.values().stream().map(Set::size).reduce(Integer::sum);
        logger.info(format("Added %s nodes for %s services", totalAdded.orElse(0), servicesIds.size()));
        logger.info("Started");
    }


    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        idsForEachService.values().forEach(Set::clear);
        idsForEachService.clear();
        logger.info("Stopped");
    }

    private Set<Long> getRelationshipsFor(IdFor<Service> serviceId) {

        Map<String, Object> params = new HashMap<>();
        params.put("serviceId", serviceId.getGraphId());
        String query = "MATCH (node:SERVICE) " +
                "WHERE node.service_id=$serviceId " +
                "RETURN ID(node) as id";

        return getNodeIdsForQuery(params, query);
    }

    public IdFor<Service> getServiceIdFor(long nodeId) {
        Set<Map.Entry<IdFor<Service>, Set<Long>>> entries = idsForEachService.entrySet();
        for (Map.Entry<IdFor<Service>, Set<Long>> entry : entries) {
            if (entry.getValue().contains(nodeId)) {
                return entry.getKey();
            }
        }
        throw new RuntimeException("Cannot find service id for node " + nodeId);
    }
}
