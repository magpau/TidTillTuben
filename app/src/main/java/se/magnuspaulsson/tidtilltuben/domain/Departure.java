package se.magnuspaulsson.tidtilltuben.domain;

import android.support.annotation.NonNull;

import java.util.Date;

/**
 * Created by magnuspaulsson on 2018-01-04.
 */

public class Departure implements Comparable {

    private String stationName;
    private String departureString;
    private Date departure;

    public Date getDeparture() {
        return departure;
    }

    public void setDeparture(Date departure) {
        this.departure = departure;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getDepartureString() {
        return departureString;
    }

    public void setDepartureString(String departureString) {
        this.departureString = departureString;
    }

    @Override
    public String toString() {
        return stationName + " " + departureString;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        Departure other = (Departure) o;
        return this.getDeparture().compareTo(other.getDeparture());
    }
}
