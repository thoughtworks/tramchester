package com.tramchester.domain.places;

import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.id.HasId;

public abstract class MapIdToDTOId<T extends GraphProperty> implements HasId<T>, IdForDTO {
    public String forDTO()  {
        return getId().forDTO();
    }
}
