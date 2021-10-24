package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;

public class BoxWithFrequencyDTO {
    private LatLong bottomLeft;
    private LatLong topRight;
    private long numberOfStopcalls;

    public BoxWithFrequencyDTO(BoundingBox boundingBox, long numberOfStopcalls) {
        this.numberOfStopcalls = numberOfStopcalls;
        bottomLeft = CoordinateTransforms.getLatLong(boundingBox.getBottomLeft());
        topRight = CoordinateTransforms.getLatLong(boundingBox.getTopRight());
    }

    public BoxWithFrequencyDTO() {
        // deserialization
    }

    public LatLong getBottomLeft() {
        return bottomLeft;
    }

    public LatLong getTopRight() {
        return topRight;
    }

    public long getNumberOfStopcalls() {
        return numberOfStopcalls;
    }
}
