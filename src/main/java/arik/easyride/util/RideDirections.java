package arik.easyride.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import arik.easyride.R;
import arik.easyride.models.Ride;
import arik.easyride.models.UserInRide;
import arik.easyride.ui.main.rides.info.MapActivity;

public class RideDirections {

    private final static String TAG = "RideDirections";
    private final static String GOOGLE_DIRECTION_API_KEY = "AIzaSyCatAgGuCDmh-XElvu8wUrlB5xX67pG_9U";
    private final static int GOOGLE_DIRECTION_WAY_POINTS_LIMIT = 8;
    private static int defaultPolylineColor;
    private static int clickedPolylineColor;

    private final GoogleMap googleMap;
    private final LatLng destinationPoint, sourcePoint;
    private Context context;
    private Ride ride;
    private List<UserInRide> participants;
    private Polyline polyline;
    private Marker directionPathInfo;

    private List<LatLng> polylineBoundaries;
    private boolean displayDirectionImmediately;

    public RideDirections(Context context, GoogleMap googleMap, Ride ride, List<UserInRide> participants) {
        this.context = context;
        this.googleMap = googleMap;
        this.ride = ride;
        this.participants = participants;

        destinationPoint = getAddressLatLng(ride.getDestination());
        sourcePoint = getAddressLatLng(ride.getSource());
    }

    public void createRoute() {
        int numberOfPoints = participants.size();
        if (numberOfPoints <= GOOGLE_DIRECTION_WAY_POINTS_LIMIT) {
            DistanceComparator comparator = new DistanceComparator(sourcePoint);
            Collections.sort(participants, comparator);

            StringBuilder sb = new StringBuilder();
            for (UserInRide participant : participants) {
                if (participant.isInRide()) {
                    if (participant.getLatitude() != null && participant.getLongitude() != null) {
                        sb.append(participant.getLatitude());
                        sb.append(",");
                        sb.append(participant.getLongitude());
                        sb.append("|");
                    }
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            String wayPoints = sb.toString();

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            String rideSource = ride.getSource();
            String rideDestination = ride.getDestination();
            String URL = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + rideSource
                    + "&destination=" + rideDestination
                    + "&mode=driving"
                    + "&waypoints=" + wayPoints
                    + "&alternatives=true"
                    + "&key=" + GOOGLE_DIRECTION_API_KEY;
            JsonObjectRequest objectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    URL, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        List<List<HashMap<String, String>>> route = new ArrayList<>();
                        List<HashMap<String, String>> path = new ArrayList<>();

                        JSONArray routeArray = response.getJSONArray("routes");
                        for (int i = 0; i < routeArray.length(); i++) {
                            JSONArray legsArray = ((JSONObject) routeArray.get(i)).getJSONArray("legs");
                            for (int j = 0; j < legsArray.length(); j++) {
                                JSONArray stepsArray = ((JSONObject) legsArray.get(j)).getJSONArray("steps");
                                String polyline = "";
                                for (int k = 0; k < stepsArray.length(); k++) {
                                    polyline = (String) ((JSONObject) ((JSONObject) stepsArray.get(k)).get("polyline")).get("points");
                                    List<LatLng> list = decodePoly(polyline);
                                    for (int l = 0; l < list.size(); l++) {
                                        HashMap<String, String> hm = new HashMap<>();
                                        hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                                        hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                                        path.add(hm);
                                    }
                                }
                            }
                            route.add(path);
                        }
                        createPolyline(route);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Direction Response error: ", error.toString());
                }
            });
            requestQueue.add(objectRequest);
        } else {
            Log.e(TAG, "Exceed points limits");
        }
    }

    private void createPolyline(List<List<HashMap<String, String>>> route) {
        ArrayList<LatLng> points = new ArrayList<>();
        PolylineOptions polylineOptions = new PolylineOptions();

        for (List<HashMap<String, String>> path : route) {
            for (HashMap<String, String> point : path) {
                String latStr = point.get("lat");
                String lngStr = point.get("lng");
                if (latStr != null && lngStr != null) {
                    double lat = Double.parseDouble(latStr);
                    double lng = Double.parseDouble(lngStr);
                    points.add(new LatLng(lat, lng));
                }
            }

            polyline = googleMap.addPolyline(polylineOptions);
            polyline.setColor(defaultPolylineColor);
            polyline.setWidth(13);
            polyline.setPoints(points);
            polyline.setClickable(true);
            polyline.setVisible(true);
            addDirectionPathInfo();

            if (displayDirectionImmediately) {
                onPolylineClick();
            }

        }
    }

    public void setPolylineBoundaries() {
        polylineBoundaries = new ArrayList<>();
        for (UserInRide participant : participants) {
            if (participant.isInRide()) {
                if (participant.getLatitude() != null && participant.getLongitude() != null) {
                    String latStr = participant.getLatitude();
                    String lngStr = participant.getLongitude();
                    if (latStr != null && lngStr != null) {
                        double lat = Double.parseDouble(latStr);
                        double lng = Double.parseDouble(lngStr);
                        polylineBoundaries.add(new LatLng(lat, lng));
                    }
                }
            }
        }
        polylineBoundaries.add(destinationPoint);
        polylineBoundaries.add(sourcePoint);
    }

    public void onPolylineClick() {
        moveCameraToPolylinePosition();
        polyline.setColor(clickedPolylineColor);
        directionPathInfo.setVisible(true);
        directionPathInfo.showInfoWindow();
    }

    public void moveCameraToPolylinePosition() {
        LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();

        for (LatLng point : polylineBoundaries) {
            boundBuilder.include(point);
        }

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen
        LatLngBounds bounds = boundBuilder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
    }

    public void clearMarkedPolyline() {
        if(polyline != null) {
            polyline.setColor(defaultPolylineColor);
            directionPathInfo.hideInfoWindow();
            directionPathInfo.setVisible(false);
        }
    }

    private void addDirectionPathInfo() {
        int middleIndex = polyline.getPoints().size() / 2;
        LatLng latLng = polyline.getPoints().get(middleIndex);

        directionPathInfo = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
                .title(ride.getName())
                .snippet(context.getString(R.string.date_colon) + ride.getDate() + " " + context.getString(R.string.time_colon) + ride.getTime()));

        directionPathInfo.setVisible(true);
        directionPathInfo.showInfoWindow();

    }

    private LatLng getAddressLatLng(String address) {
        List<Address> addresses;
        if (context != null) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                addresses = geocoder.getFromLocationName(address, 1);
                if (addresses.isEmpty())
                    return null;
                double lat = addresses.get(0).getLatitude();
                double lng = addresses.get(0).getLongitude();
                return new LatLng(lat, lng);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    public List<LatLng> getPolylineBoundaries() {
        return polylineBoundaries;
    }

    public void setDefaultPolylineColor(int defaultPolylineColor) {
        RideDirections.defaultPolylineColor = defaultPolylineColor;
    }

    public void setClickedPolylineColor(int clickedPolylineColor) {
        RideDirections.clickedPolylineColor = clickedPolylineColor;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public List<UserInRide> getParticipants() {
        return participants;
    }

    public void setParticipants(List<UserInRide> participants) {
        this.participants = participants;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public void setDisplayDirectionImmediately(boolean displayDirectionImmediately) {
        this.displayDirectionImmediately = displayDirectionImmediately;
    }

    public Marker getDirectionPathInfo() {
        return directionPathInfo;
    }
}
