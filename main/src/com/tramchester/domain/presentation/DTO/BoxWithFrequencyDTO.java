package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;

import java.util.List;

@JsonTypeName("BoxWithFrequency")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use=JsonTypeInfo.Id.NAME)
public class BoxWithFrequencyDTO {
    private LatLong bottomLeft;
    private LatLong topRight;
    private long numberOfStopcalls;
    private List<StationRefDTO> stops;

    public BoxWithFrequencyDTO(BoundingBox boundingBox, List<StationRefDTO> stops, long numberOfStopcalls) {
        this.stops = stops;
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

    public List<StationRefDTO> getStops() {
        return stops;
    }
}
