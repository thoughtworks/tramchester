package com.tramchester.graph.graphbuild;

import com.google.common.collect.Streams;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.StringIdFor.getIdFromGraphEntity;
import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphProps {

    public static void setProp(Node node, DataSourceInfo dataSourceInfo) {
        DataSourceID sourceID = dataSourceInfo.getID();
        node.setProperty(sourceID.name(), dataSourceInfo.getVersion());
    }

    public static <C extends GraphProperty & CoreDomain & HasId<C>> void setProperty(Entity entity, C item) {
        entity.setProperty(item.getProp().getText(), item.getId().getGraphId());
    }

    public static void setEndDate(Entity entity, TramDate date) {
        setEndDate(entity, date.toLocalDate());
    }

    public static void setEndDate(Entity entity, LocalDate date) {
        entity.setProperty(END_DATE.getText(), date);
    }

    public static LocalDate getEndDate(Relationship relationship) {
        return (LocalDate) relationship.getProperty(END_DATE.getText());
    }

    public static void setStartDate(Entity entity, TramDate date) {
        setStartDate(entity, date.toLocalDate());
    }

    public static void setStartDate(Entity entity, LocalDate date) {
        entity.setProperty(START_DATE.getText(), date);
    }

    public static LocalDate getStartDate(Relationship relationship) {
        return (LocalDate) relationship.getProperty(START_DATE.getText());
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

        short[] replacement = Arrays.copyOf(existing, existing.length + 1);
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

    // TODO Change to seconds, not minutes
    public static Duration getCost(Entity entity) {
        final int value = (int) getProperty(entity, COST);
        return Duration.ofMinutes(value);
    }

    // TODO Change to seconds, not minutes
    public static void setCostProp(Entity entity, Duration duration) {
        int minutes = roundUpNearestMinute(duration);
        entity.setProperty(COST.getText(), minutes);
    }

    // TODO Change to seconds, not minutes
    public static void setMaxCostProp(Entity entity, Duration duration) {
        int minutes = roundUpNearestMinute(duration);
        entity.setProperty(MAX_COST.getText(), minutes);
    }

    private static int roundUpNearestMinute(Duration duration) {
        @SuppressWarnings("WrapperTypeMayBePrimitive")
        Double minutes = Math.ceil(duration.toSeconds()/60D);
        return minutes.intValue();
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
        final double lat = (double) getProperty(entity, LATITUDE);
        final double lon = (double) getProperty(entity, LONGITUDE);
        return new LatLong(lat, lon);
    }

    public static void setWalkId(Entity entity, LatLong origin, UUID uid) {
        entity.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
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
        String value = entity.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    public static IdFor<Platform> getPlatformIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, PLATFORM_ID);
    }

    public static EnumSet<GraphLabel> getLabelsFor(Node node) {
        final Iterable<Label> iter = node.getLabels();

        final Set<GraphLabel> set = Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toSet());
        return EnumSet.copyOf(set);
    }

    public static IdFor<NaptanArea> getAreaIdFromGrouped(Entity entity) {
        return getIdFromGraphEntity(entity, AREA_ID);
    }

    public static void setProperty(Entity entity, IdFor<NaptanArea> areaId) {
        entity.setProperty(AREA_ID.getText(), areaId.getGraphId());
    }

    public static boolean validOn(TramDate date, Relationship relationship) {
        return validOn(date.toLocalDate(), relationship);
    }

    public static boolean validOn(LocalDate date, Relationship relationship) {
        LocalDate startDate = getStartDate(relationship);
        if (date.isBefore(startDate)) {
            return false;
        }
        LocalDate endDate = getEndDate(relationship);
        if (date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    public static void setDateRange(Entity entity, DateRange range) {
        setStartDate(entity, range.getStartDate().toLocalDate());
        setEndDate(entity, range.getEndDate().toLocalDate());
    }
}