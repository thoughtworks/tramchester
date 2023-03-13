package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.id.serialization.IdForDTOSerialization;

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
