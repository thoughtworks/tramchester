package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;

public class BoxDTO {
    private LatLong bottomLeft;
    private LatLong topRight;

    public BoxDTO(BoundingBox box) {
        this(CoordinateTransforms.getLatLong(box.getBottomLeft()), CoordinateTransforms.getLatLong(box.getTopRight()));
    }

    public BoxDTO(LatLong bottomLeft, LatLong topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public BoxDTO() {
        // deserialisation
    }

    public LatLong getBottomLeft() {
        return bottomLeft;
    }

    public LatLong getTopRight() {
        return topRight;
    }
}
