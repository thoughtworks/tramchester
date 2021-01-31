package com.tramchester.graph.graphbuild;

import com.tramchester.domain.*;
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

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphProps {

    static void setProp(Node node, DataSourceInfo dataSourceInfo) {
        DataSourceID name = dataSourceInfo.getID();
        node.setProperty(name.getName(), dataSourceInfo.getVersion());
    }

    public static <C extends GraphProperty>  void setProperty(Entity entity, HasId<C> item) {
        entity.setProperty(item.getProp().getText(), item.getId().getGraphId());
    }

    public static <C extends GraphProperty>  void setProperty(Entity entity, TransportMode mode) {
        entity.setProperty(TRANSPORT_MODE.getText(), mode.getNumber());
    }

    public static TransportMode getTransportMode(Entity entity) {
        short number = (short) entity.getProperty(TRANSPORT_MODE.getText());
        return TransportMode.fromNumber(number);
    }

    public static Set<TransportMode> getTransportModes(Entity entity) {
        if (!entity.hasProperty(TRANSPORT_MODES.getText())) {
            return Collections.emptySet();
        }

        short[] existing = (short[]) entity.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    public static void addTransportMode(Entity entity, TransportMode mode) {
        if (!(entity.hasProperty(TRANSPORT_MODES.getText()))) {
            entity.setProperty(TRANSPORT_MODES.getText(), new short[]{mode.getNumber()});
            return;
        }

        short[] existing = (short[]) entity.getProperty(TRANSPORT_MODES.getText());
        short[] replacement =  Arrays.copyOf(existing, existing.length+1);
        replacement[existing.length] = mode.getNumber();
        entity.setProperty(TRANSPORT_MODES.getText(), replacement);
    }

    public static IdFor<Station> getStationId(Entity entity) {
        return IdFor.getStationIdFrom(entity);
    }

    public static void setRouteStationProp(Entity entity, IdFor<RouteStation> id) {
        entity.setProperty(ROUTE_STATION_ID.getText(), id.getGraphId());
    }

    public static void setTowardsProp(Node node, IdFor<Station> id) {
        node.setProperty(TOWARDS_STATION_ID.getText(), id.getGraphId());
    }

    private static Object getProperty(Entity node, GraphPropertyKey graphPropertyKey) {
        return node.getProperty(graphPropertyKey.getText());
    }

    public static String getTrips(Entity relationship) {
        return getProperty(relationship, TRIPS).toString();
    }

    static void setTripsProp(Entity relationship, String value) {
        relationship.setProperty(TRIPS.getText(), value);
    }

    public static int getCost(Entity relationship) {
        return (int) getProperty(relationship, COST);
    }

    public static void setCostProp(Entity relationship, int value) {
        relationship.setProperty(COST.getText(), value);
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
        return IdFor.getTripIdFrom(entity);
    }

    public static IdFor<Service> getServiceId(Entity entity) {
        return IdFor.getServiceIdFrom(entity);
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

    public static IdFor<Route> getRouteId(Entity entity) {
        return IdFor.getRouteIdFrom(entity);
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
}
