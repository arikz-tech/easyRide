package arikz.easyride.util;

import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

import arikz.easyride.models.UserInRide;

public class DistanceComparator implements Comparator<UserInRide> {
    private double currentLat;
    private double currentLng;

    public DistanceComparator(LatLng current) {
        currentLat = current.latitude;
        currentLng = current.longitude;
    }

    @Override
    public int compare(UserInRide first, UserInRide second) {
        if(first.isInRide() && !second.isInRide()){
            return -1;
        }

        if(!first.isInRide() && second.isInRide()){
            return 1;
        }

        if (first.isInRide() && second.isInRide()) {
            if (first.getLongitude() != null && first.getLatitude() != null && second.getLatitude() != null && second.getLongitude() != null) {
                double firstLat = Double.parseDouble(first.getLatitude());
                double firstLng = Double.parseDouble(first.getLongitude());
                double secondLat = Double.parseDouble(second.getLatitude());
                double secondLng = Double.parseDouble(second.getLongitude());

                double distanceToFirst = distance(currentLat, currentLng, firstLat, firstLng);
                double distanceToSecond = distance(currentLat, currentLng, secondLat, secondLng);

                return (int) (distanceToFirst - distanceToSecond);
            } else
                return 0;
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
