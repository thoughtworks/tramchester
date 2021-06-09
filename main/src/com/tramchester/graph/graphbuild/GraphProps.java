package com.tramchester.graph.graphbuild;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.tramchester.domain.id.StringIdFor.getCompositeIdFromGraphEntity;
import static com.tramchester.domain.id.StringIdFor.getIdFromGraphEntity;
import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphProps {

    static void setProp(Node node, DataSourceInfo dataSourceInfo) {
        DataSourceID name = dataSourceInfo.getID();
        node.setProperty(name.getName(), dataSourceInfo.getVersion());
    }

    public static <C extends GraphProperty>  void setProperty(Entity entity, HasId<C> item) {
        entity.setProperty(item.getProp().getText(), item.getId().getGraphId());
    }

    public static void setProperty(Entity entity, TransportMode mode) {
        entity.setProperty(TRANSPORT_MODE.getText(), mode.getNumber());
    }

    public static TransportMode getTransportMode(Entity entity) {
        short number = (short) entity.getProperty(TRANSPORT_MODE.getText());
        return TransportMode.fromNumber(number);
    }

    // TODO - auto conversation to/from ENUM arrays now available?
    public static Set<TransportMode> getTransportModes(Entity entity) {
        if (!entity.hasProperty(TRANSPORT_MODES.getText())) {
            return Collections.emptySet();
        }

        short[] existing = (short[]) entity.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    public static void addTransportMode(Entity entity, TransportMode mode) {
        short modeNumber = mode.getNumber();
        if (!(entity.hasProperty(TRANSPORT_MODES.getText()))) {
            entity.setProperty(TRANSPORT_MODES.getText(), new short[]{modeNumber});
            return;
        }

        short[] existing = (short[]) entity.getProperty(TRANSPORT_MODES.getText());
        for (short value : existing) {
            if (value == modeNumber) {
                return;
            }
        }

        short[] replacement =  Arrays.copyOf(existing, existing.length+1);
        replacement[existing.length] = modeNumber;
        entity.setProperty(TRANSPORT_MODES.getText(), replacement);
    }

    public static IdFor<Station> getStationId(Entity entity) {
        return getStationIdFrom(entity);
    }

    public static void setRouteStationProp(Entity entity, IdFor<RouteStation> id) {
        entity.setProperty(ROUTE_STATION_ID.getText(), id.getGraphId());
    }

    public static void setTowardsProp(Node node, IdFor<Station> id) {
        node.setProperty(TOWARDS_STATION_ID.getText(), id.getGraphId());
    }

    private static Object getProperty(Entity entity, GraphPropertyKey graphPropertyKey) {
        return entity.getProperty(graphPropertyKey.getText());
    }

    public static IdSet<Trip> getTrips(Entity entity) {
        if (!hasProperty(TRIPS, entity)) {
            return IdSet.emptySet(); // ok during graph build when property not set initiall
        }

        String[] ids = (String[]) getProperty(entity, TRIPS);
        IdSet<Trip> tripIds = new IdSet<>(ids.length);
        for (String id : ids) {
            tripIds.add(StringIdFor.createId(id));
        }
        return tripIds;
    }

    static void setTripsProp(Entity entiy, IdSet<Trip> tripIds) {
        String[] ids = new String[tripIds.size()];
        int i = 0;
        for (IdFor<Trip> tripId : tripIds) {
            ids[i++] = tripId.getGraphId();
        }
        entiy.setProperty(TRIPS.getText(), ids);
    }
    
    public static int getCost(Entity entity) {
        return (int) getProperty(entity, COST);
    }

    public static void setCostProp(Entity entity, int value) {
        entity.setProperty(COST.getText(), value);
    }

    public static TramTime getTime(Entity entity) {
        LocalTime localTime = (LocalTime) getProperty(entity, TIME);
        boolean nextDay = entity.hasProperty(DAY_OFFSET.getText());
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }

    static void setTimeProp(Entity entity, TramTime time) {
        entity.setProperty(TIME.getText(), time.asLocalTime());
        if (time.isNextDay()) {
            entity.setProperty(DAY_OFFSET.getText(), time.isNextDay());
        }
    }

    public static boolean hasProperty(GraphPropertyKey key, Entity entity) {
        return entity.hasProperty(key.getText());
    }

    public static IdFor<Trip> getTripId(Entity entity) {
        return getTripIdFrom(entity);
    }

    public static IdFor<Service> getServiceId(Entity entity) {
        return getServiceIdFrom(entity);
    }

    static void setHourProp(Entity entity, Integer value) {
        entity.setProperty(HOUR.getText(), value);
    }

    public static Integer getHour(Entity node) {
        return (int) getProperty(node, HOUR);
    }

    public static void setLatLong(Entity entity, LatLong latLong) {
        entity.setProperty(LATITUDE.getText(), latLong.getLat());
        entity.setProperty(LONGITUDE.getText(), latLong.getLon());
    }

    public static LatLong getLatLong(Entity entity) {
        double lat = (double) getProperty(entity, LATITUDE);
        double lon = (double) getProperty(entity, LONGITUDE);
        return new LatLong(lat, lon);
    }


    public static void setWalkId(Entity entity, LatLong origin, UUID uid) {
        entity.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString()+"_"+uid.toString());
    }


    public static void setStopSequenceNumber(Relationship relationship, int stopSequenceNumber) {
        relationship.setProperty(STOP_SEQ_NUM.getText(), stopSequenceNumber);
    }

    public static int getStopSequenceNumber(Relationship relationship) {
        return (int) relationship.getProperty(STOP_SEQ_NUM.getText());
    }

    public static IdFor<Route> getRouteIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, ROUTE_ID);
    }

    public static IdFor<Station> getStationIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, STATION_ID);
    }

    public static IdFor<Station> getTowardsStationIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, TOWARDS_STATION_ID);
    }

    public static IdFor<Service> getServiceIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, SERVICE_ID);
    }

    public static IdFor<Trip> getTripIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, TRIP_ID);
    }

    public static IdFor<RouteStation> getRouteStationIdFrom(Entity entity) {
        return getCompositeIdFromGraphEntity(entity, ROUTE_STATION_ID);
    }

    public static IdFor<Platform> getPlatformIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, PLATFORM_ID);
    }

}
