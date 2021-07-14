package arik.easyride.util;

import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

import arik.easyride.models.UserInRide;

public class DistanceComparator implements Comparator<LatLng> {
    private double currentLat;
    private double currentLng;

    public DistanceComparator(LatLng current) {
        currentLat = current.latitude;
        currentLng = current.longitude;
    }

    @Override
    public int compare(LatLng first, LatLng second) {
        if (first != null && second != null) {
            double distanceToFirst = distance(currentLat, currentLng, first.latitude, first.longitude);
            double distanceToSecond = distance(currentLat, currentLng, second.latitude, second.longitude);
            return (int) (distanceToFirst - distanceToSecond);
        }
        return 0;
    }

    public double distance(double fromLat, double fromLon, double toLat, double toLon) {
        double radius = 6378137;   // approximate Earth radius, *in meters*
        double deltaLat = toLat - fromLat;
        double deltaLon = toLon - fromLon;
        double angle = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(deltaLat / 2), 2) +
                        Math.cos(fromLat) * Math.cos(toLat) *
                                Math.pow(Math.sin(deltaLon / 2), 2)));
        return radius * angle;
    }
}
