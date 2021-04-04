package arik.easyride.ui.main.rides.info;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import arik.easyride.R;
import arik.easyride.models.Ride;
import arik.easyride.models.UserInRide;
import arik.easyride.util.ClusterMarker;
import arik.easyride.util.DistanceComparator;
import arik.easyride.util.UserClusterManagerRenderer;
import arik.easyride.util.UserMarkerManager;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {
    private static final String TAG = ".MapActivity";
    private static final int LOCATION_REQUEST_CODE = 125;
    private GoogleMap mGoogleMap;
    private HashMap<String, Polyline> polyLines;
    private HashMap<String, Marker> pathInfo;
    private HashMap<String, List<LatLng>> pathBounds;
    private Ride ride;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        ride = getIntent().getParcelableExtra("ride");

        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.night_map_style));
        }

        updateRidesRoutes();
        mGoogleMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
        Polyline clickedPath = polyLines.get(polyline.getId());
        Marker clickedPathInfo = pathInfo.get(polyline.getId());
        if (clickedPath != null && clickedPathInfo != null) {
            clickedPath.setColor(getColor(R.color.amber_500));
            clickedPath.setWidth(15);
            clickedPathInfo.setVisible(true);
            clickedPathInfo.showInfoWindow();

            List<LatLng> boundPoints = pathBounds.get(polyline.getId());
            if (boundPoints != null) {
                for (LatLng bounds : boundPoints) {
                    boundBuilder.include(bounds);
                }
                boundBuilder.include(polyline.getPoints().get(0));
            }
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen
            LatLngBounds bounds = boundBuilder.build();
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));

            for (Map.Entry<String, Polyline> path : polyLines.entrySet()) {
                Polyline line = polyLines.get(path.getKey());
                if (line != null) {
                    if (!line.equals(clickedPath)) {
                        line.setColor(getColor(R.color.black));
                        line.setWidth(13);
                    }
                }
            }

            for (Map.Entry<String, Marker> path : pathInfo.entrySet()) {
                Marker info = pathInfo.get(path.getKey());
                if (info != null) {
                    if (!info.equals(clickedPathInfo)) {
                        info.setVisible(false);
                    }
                }
            }
        }
    }

    private void updateRidesRoutes() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserInRide> participants = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    participants.add(snap.getValue(UserInRide.class));
                }

                for (UserInRide participant : participants) {
                    if (participant.isInRide()) {
                        createRideRoute(ride.getRid(), participants);
                        addUsersMarker(participants);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void createRideRoute(String rid, final List<UserInRide> participants) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rides").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Ride ride = snapshot.getValue(Ride.class);
                if (ride != null) {
                    String src = ride.getSource();
                    String dest = ride.getDestination();
                    String name = ride.getName();
                    String date = ride.getDate();
                    String time = ride.getTime();

                    LatLng srcLatLng = getAddressLatLng(MapActivity.this, src);
                    if (srcLatLng != null) {
                        DistanceComparator comparator = new DistanceComparator(srcLatLng);
                        Collections.sort(participants, comparator);
                        createRoute(name, src, dest, date, time, participants);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private LatLng getAddressLatLng(Context context, String address) {
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

    private void createRoute(final String name, String src, final String dest, final String date, final String time, final List<UserInRide> participants) {
        StringBuilder sb = new StringBuilder();
        int googleLimit = 8;
        for (UserInRide participant : participants) {
            if (participant.isInRide()) {
                if (googleLimit < 1)
                    break;
                if (participant.getLatitude() != null && participant.getLongitude() != null) {
                    sb.append(participant.getLatitude());
                    sb.append(",");
                    sb.append(participant.getLongitude());
                    sb.append("|");
                    googleLimit--;
                }
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        String wayPoints = sb.toString();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String URL = "https://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + src
                + "&destination=" + dest
                + "&mode=driving"
                + "&waypoints=" + wayPoints
                + "&alternatives=true"
                + "&key=" + getString(R.string.google_direction_api);
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
                    addPolyline(name, date, time, dest, participants, route);
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

    private void addPolyline(String name, String date, String time, String dest, List<UserInRide> participants, List<List<HashMap<String, String>>> route) {
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

            if (polyLines == null) {
                polyLines = new HashMap<>();
            }

            Polyline polyline = mGoogleMap.addPolyline(polylineOptions);
            polyLines.put(polyline.getId(), polyline);

            polyline.setColor(getColor(R.color.black));
            polyline.setWidth(13);
            polyline.setPoints(points);
            polyline.setClickable(true);
            addPathInfo(name, date, time, polyline);

            if (pathBounds == null) {
                pathBounds = new HashMap<>();
            }

            int googleLimit = 8;
            List<LatLng> boundPoints = new ArrayList<>();
            for (UserInRide participant : participants) {
                if (participant.isInRide()) {
                    if (googleLimit < 1)
                        break;
                    if (participant.getLatitude() != null && participant.getLongitude() != null) {
                        String latStr = participant.getLatitude();
                        String lngStr = participant.getLongitude();
                        if (latStr != null && lngStr != null) {
                            double lat = Double.parseDouble(latStr);
                            double lng = Double.parseDouble(lngStr);
                            boundPoints.add(new LatLng(lat, lng));
                        }
                    }
                    googleLimit--;
                }
            }
            //Add destination point.
            boundPoints.add(getAddressLatLng(this, dest));
            pathBounds.put(polyline.getId(), boundPoints);

            onPolylineClick(polyline);
        }
    }

    private void addPathInfo(String name, String date, String time, Polyline polyline) {
        int middleIndex = polyline.getPoints().size() / 2;
        LatLng latLng = polyline.getPoints().get(middleIndex);
        if (pathInfo == null) {
            pathInfo = new HashMap<>();
        }
        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
                .title(name)
                .snippet(getString(R.string.date_colon) + date + " " + getString(R.string.time_colon) + time));
        marker.setVisible(false);
        pathInfo.put(polyline.getId(), marker);
    }

    private void addUsersMarker(List<UserInRide> participants) {
        if (mGoogleMap != null) {
            ClusterManager<ClusterMarker> clusterManager = new ClusterManager<>(this, mGoogleMap);
            UserClusterManagerRenderer clusterManagerRenderer = new UserClusterManagerRenderer(this, mGoogleMap, clusterManager);
            clusterManager.setRenderer(clusterManagerRenderer);
            UserMarkerManager userMarkerManager = new UserMarkerManager(this, clusterManager);
            if (participants != null) {
                for (UserInRide user : participants) {
                    if (user.isInRide()) {
                        userMarkerManager.addToMap(user);
                    }
                }
            }
        }
    }

}