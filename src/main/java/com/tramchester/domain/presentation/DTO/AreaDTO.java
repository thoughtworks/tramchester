package com.tramchester.domain.presentation.DTO;

public class AreaDTO {
    private String areaName;

    public AreaDTO() {
        // deserialisation
    }

    public AreaDTO(String areaName) {

        this.areaName = areaName;
    }

    public String getAreaName() {
        // serialisation
        return areaName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AreaDTO areaDTO = (AreaDTO) o;

        return areaName.equals(areaDTO.areaName);
    }

    @Override
    public int hashCode() {
        return areaName.hashCode();
    }

}
