package com.tramchester.domain.places;

import com.tramchester.domain.HasId;

public abstract class MapIdToDTOId<T> implements HasId<T>, IdForDTO {
    public String forDTO()  {
        return getId().forDTO();
    }
}
