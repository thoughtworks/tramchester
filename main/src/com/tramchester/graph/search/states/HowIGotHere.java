package com.tramchester.graph.search.states;

import org.neo4j.graphdb.Path;

public class HowIGotHere {

    private static final long AT_START = Long.MIN_VALUE;
    private final long relationshipId;
    private final long nodeId;

    public HowIGotHere(Path path) {
        nodeId = path.endNode().getId();

        if (path.lastRelationship()==null) {
            relationshipId = AT_START;
        } else {
            relationshipId = path.lastRelationship().getId();
        }
    }

    private HowIGotHere(long nodeId, long relationshipId) {
        this.nodeId = nodeId;
        this.relationshipId = relationshipId;
    }

    public static HowIGotHere None() {
        return new HowIGotHere(-1,-1);
    }

    public static HowIGotHere forTest(long nodeId, long relationshipId) {
        return new HowIGotHere(nodeId, relationshipId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HowIGotHere that = (HowIGotHere) o;

        if (relationshipId != that.relationshipId) return false;
        return nodeId == that.nodeId;
    }

    @Override
    public int hashCode() {
        int result = (int) (relationshipId ^ (relationshipId >>> 32));
        result = 31 * result + (int) (nodeId ^ (nodeId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "HowIGotHere{" +
                "relationshipId=" + relationshipId +
                ", nodeId=" + nodeId +
                '}';
    }

    public long getEndNodeId() {
        return nodeId;
    }

    public long getRelationshipId() {
        return relationshipId;
    }

    public boolean atStart() {
        return relationshipId==AT_START;
    }
}
