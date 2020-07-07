package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import org.opengis.referencing.operation.TransformException;

@JsonTypeName("BoxWithCost")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class BoxWithCostDTO {

    private LatLong bottomLeft;
    private LatLong topRight;
    private int minutes;

    private BoxWithCostDTO(LatLong bottomLeft, LatLong topRight, int minutes) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.minutes = minutes;
    }

    public BoxWithCostDTO() {
        // deserialisation
    }

    public static BoxWithCostDTO createFrom(CoordinateTransforms transforms, BoundingBoxWithCost box) throws TransformException {
        LatLong bottomLeft = transforms.getLatLong(box.getMinEastings(), box.getMinNorthings());
        LatLong topRight = transforms.getLatLong(box.getMaxEasting(), box.getMaxNorthings());
        return new BoxWithCostDTO(bottomLeft, topRight, box.getMinutes());
    }

    public int getMinutes() {
        return minutes;
    }

    public LatLong getBottomLeft() {
        return bottomLeft;
    }

    public LatLong getTopRight() {
        return topRight;
    }
}
