package com.tramchester.graph;


import com.tramchester.domain.RawStage;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.PathToTransportRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphalgo.WeightedPath;

import java.time.LocalTime;
import java.util.List;

public class MapPathToStages {
    private PathToTransportRelationship pathToTransportRelationship;
    private MapTransportRelationshipsToStages mapTransportRelationshipsToStages;

    public MapPathToStages(PathToTransportRelationship pathToTransportRelationship, MapTransportRelationshipsToStages mapTransportRelationshipsToStages) {
        this.pathToTransportRelationship = pathToTransportRelationship;
        this.mapTransportRelationshipsToStages = mapTransportRelationshipsToStages;
    }

    public List<RawStage> map(WeightedPath path, LocalTime minsPastMidnight) throws TramchesterException {
        List<TransportRelationship> relationships = pathToTransportRelationship.mapPath(path);
        return mapTransportRelationshipsToStages.mapStages(relationships, minsPastMidnight);
    }
 }
