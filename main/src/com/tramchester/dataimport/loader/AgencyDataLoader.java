package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.AgencyData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public class AgencyDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(AgencyDataLoader.class);

    private final DataSourceID dataSourceID;
    private final TransportEntityFactory factory;

    public AgencyDataLoader(DataSourceInfo dataSourceInfo, TransportEntityFactory factory) {
        this.dataSourceID = dataSourceInfo.getID();
        this.factory = factory;
    }

    public CompositeIdMap<Agency, MutableAgency> load(Stream<AgencyData> agencyDataStream) {
        logger.info("Loading all agencies for " + dataSourceID);
        CompositeIdMap<Agency, MutableAgency> agencies = new CompositeIdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(factory.createAgency(dataSourceID, agencyData)));
        logger.info("Loaded " + agencies.size() + " agencies for " + dataSourceID);
        return agencies;
    }
}
