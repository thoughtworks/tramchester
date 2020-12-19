package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.mappers.TramJourneyToDTOMapper;
import org.opengis.referencing.operation.TransformException;

@JsonTypeName("BoxWithCost")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class BoxWithCostDTO {

    private LatLong bottomLeft;
    private LatLong topRight;
    private int minutes;
    private JourneyDTO journey;

    private BoxWithCostDTO(LatLong bottomLeft, LatLong topRight, int minutes, JourneyDTO journey) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.minutes = minutes;
        this.journey = journey;
    }

    @SuppressWarnings("unused")
    public BoxWithCostDTO() {
        // deserialisation
    }

    public static BoxWithCostDTO createFrom(TramJourneyToDTOMapper mapper, TramServiceDate serviceDate,
                                            BoundingBoxWithCost box) throws TransformException {

        // TODO Assuming valid positions here
        LatLong bottomLeft = CoordinateTransforms.getLatLong(box.getBottomLeft());
        LatLong topRight = CoordinateTransforms.getLatLong(box.getTopRight());

        if (box.getJourney()!=null) {
            return new BoxWithCostDTO(bottomLeft, topRight, box.getMinutes(), mapper.createJourneyDTO(box.getJourney(), serviceDate));
        } else {
            return new BoxWithCostDTO(bottomLeft, topRight, box.getMinutes(), null);
        }
    }

    public int getMinutes() {
        return minutes;
    }

    @SuppressWarnings("unused")
    public LatLong getBottomLeft() {
        return bottomLeft;
    }

    @SuppressWarnings("unused")
    public LatLong getTopRight() {
        return topRight;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public JourneyDTO getJourney() {
        return journey;
    }

    @Override
    public String toString() {
        return "BoxWithCostDTO{" +
                "bottomLeft=" + bottomLeft +
                ", topRight=" + topRight +
                ", minutes=" + minutes +
                ", journey=" + journey +
                '}';
    }
}
