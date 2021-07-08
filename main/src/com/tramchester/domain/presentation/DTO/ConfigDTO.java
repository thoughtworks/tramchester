package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.reference.TransportMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigDTO {
    private List<TransportMode> modes;
    private boolean postcodesEnabled;
    private int numberJourneysToDisplay;

    public ConfigDTO(Collection<TransportMode> modes, boolean postcodesEnabled, int numberJourneysToDisplay) {
        this.modes = new ArrayList<>(modes);
        this.postcodesEnabled = postcodesEnabled;
        this.numberJourneysToDisplay = numberJourneysToDisplay;
    }

    public ConfigDTO() {
        // deserialisation
    }

    public List<TransportMode> getModes() {
        return modes;
    }

    public boolean getPostcodesEnabled() {
        return postcodesEnabled;
    }

    public int getNumberJourneysToDisplay() {
        return numberJourneysToDisplay;
    }
}
