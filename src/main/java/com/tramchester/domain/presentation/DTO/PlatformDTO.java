package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Platform;

public class PlatformDTO {

    private String id;
    private String name;
    private String platformNumber;

    public PlatformDTO() {
        // for deserialisation
    }

    public PlatformDTO(Platform original) {
        this.id = original.getId();
        this.name = original.getName();
        this.platformNumber = original.getPlatformNumber();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlatformNumber() {
        return platformNumber;
    }
}
