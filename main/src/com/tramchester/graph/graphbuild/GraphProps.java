package com.tramchester.graph.graphbuild;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;
import java.util.UUID;

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphProps {

    static void setProp(Node node, DataSourceInfo dataSourceInfo) {
        node.setProperty(dataSourceInfo.getName(), dataSourceInfo.getVersion());
    }

    public static <C extends GraphProperty>  void setProperty(Entity entity, HasId<C> item) {
        entity.setProperty(item.getProp().getText(), item.getId().getGraphId());
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

    private static Object getProperty(Entity node, GraphPropertyKey longitude) {
        return node.getProperty(longitude.getText());
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

    static void setTimeProp(Entity entity, ServiceTime time) {
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

}
