package com.tramchester.integration.graph;


import com.tramchester.domain.RawStage;
import com.tramchester.integration.graph.Relationships.PathToTransportRelationship;
import com.tramchester.integration.graph.Relationships.TransportRelationship;
import org.neo4j.graphalgo.WeightedPath;

import java.util.List;

public class MapPathToStages {
    private PathToTransportRelationship pathToTransportRelationship;
    private MapTransportRelationshipsToStages mapTransportRelationshipsToStages;

    public MapPathToStages(PathToTransportRelationship pathToTransportRelationship, MapTransportRelationshipsToStages mapTransportRelationshipsToStages) {
        this.pathToTransportRelationship = pathToTransportRelationship;
        this.mapTransportRelationshipsToStages = mapTransportRelationshipsToStages;
    }

    public List<RawStage> map(WeightedPath path, int minsPastMidnight) {
        List<TransportRelationship> relationships = pathToTransportRelationship.mapPath(path);
        return mapTransportRelationshipsToStages.mapStages(relationships, minsPastMidnight);
    }
 }
