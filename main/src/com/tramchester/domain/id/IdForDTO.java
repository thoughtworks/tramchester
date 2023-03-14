package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.serialization.IdForDTOSerialization;
import com.tramchester.domain.presentation.LatLong;

import java.util.Objects;

@JsonSerialize(using = IdForDTOSerialization.class)
public class IdForDTO {
    private final String actualId;

    public IdForDTO(IdFor<?> sourceId) {
        if (!sourceId.isValid()) {
            throw new RuntimeException("Invalid source id " + sourceId);
        }

        if (sourceId instanceof StringIdFor) {
            StringIdFor<?> stringId = (StringIdFor<?>) sourceId;
            actualId = stringId.getContainedId();
        } else if (sourceId instanceof PlatformId) {
            PlatformId platformId = (PlatformId) sourceId;
            StringIdFor<Platform> internalId = platformId.getContainedId();
            actualId = internalId.getContainedId();
        } else if (sourceId instanceof RailRouteId) {
            RailRouteId railRouteId = (RailRouteId) sourceId;
            StringIdFor<Route> internalId = railRouteId.getContainedId();
            actualId = internalId.getContainedId();
        } else {
            throw new RuntimeException("Not defined for " + sourceId);
        }
    }

    public IdForDTO(String id) {
        this.actualId = id;
    }

    public static IdForDTO createFor(HasId<?> hasId) {
        return new IdForDTO(hasId.getId());
    }

    /***
     * Pseudo id for locations
     * TODO should this be the MyLocationId?
     * @param latLon the position
     * @return a IdForDTO
     */
    public static IdForDTO createFor(LatLong latLon) {
        String latLong = String.format("%s,%s", latLon.getLat(), latLon.getLon());
        return new IdForDTO(latLong);
    }

    public String getActualId() {
        return actualId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdForDTO idForDTO = (IdForDTO) o;
        return actualId.equals(idForDTO.actualId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualId);
    }

    @Override
    public String toString() {
        return "IdForDTO{" +
                "actualId='" + actualId + '\'' +
                '}';
    }
}
