package com.tramchester.dataimport.NaPTAN.xml;

import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

public interface NaptanStopData extends HasGridPosition {
    String getAtcoCode();

    String getNaptanCode();

    GridPosition getGridPosition();

    NaptanStopType getStopType();

    String getCommonName();

    String getSuburb();

    String getTown();

    String getIndicator();
}
