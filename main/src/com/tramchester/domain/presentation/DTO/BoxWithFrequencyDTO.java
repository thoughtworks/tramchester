package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;

import java.util.List;

@JsonTypeName("BoxWithFrequency")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use=JsonTypeInfo.Id.NAME)
public class BoxWithFrequencyDTO {
    private List<TransportMode> modes;
    private LatLong bottomLeft;
    private LatLong topRight;
    private long numberOfStopcalls;
    private List<LocationRefDTO> stops;

    public BoxWithFrequencyDTO(BoundingBox boundingBox, List<LocationRefDTO> stops, long numberOfStopcalls,
                               List<TransportMode> modes) {
        this.stops = stops;
        this.numberOfStopcalls = numberOfStopcalls;
        this.modes = modes;
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

    public List<LocationRefDTO> getStops() {
        return stops;
    }

    public List<TransportMode> getModes() { return modes; }
}
