package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

import java.util.Objects;

import static com.tramchester.domain.places.Station.TRAM_STATION_POSTFIX;

public class StopData {

    @JsonProperty("stop_id")
    private String id;
    @JsonProperty("stop_code")
    private String code;
    @JsonProperty("stop_lat")
    private double latitude;
    @JsonProperty("stop_lon")
    private double longitude;

    private String area = "";
    private String name;

    // deserialization
    public StopData() {

    }

    @JsonProperty("stop_name")
    private void setName(String text) {
        text = text.replace("\"", "").trim();

        boolean isMetrolink = text.endsWith(TRAM_STATION_POSTFIX);

        if (isMetrolink) {
            setMetrolinkNameAndArea(text);
        } else {
            setNameAndArea(text);
        }
    }

    private void setNameAndArea(String text) {
        int indexOfDivider = text.indexOf(',');
        if (indexOfDivider > 0) {
            this.area = text.substring(0, indexOfDivider);
        }
        this.name = text;
    }

    private void setMetrolinkNameAndArea(String text) {
        int indexOfDivider = text.indexOf(',');
        this.area = text.substring(0, indexOfDivider);
        this.name = text.substring(indexOfDivider+1, text.length());
        this.name = name.replace(TRAM_STATION_POSTFIX, "").trim();
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public boolean isTFGMTram() {
        return id.startsWith(Station.METROLINK_PREFIX);
    }

    public LatLong getLatLong() {
        return new LatLong(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopData stopData = (StopData) o;
        return Objects.equals(id, stopData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StopData{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", area='" + area + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

}
