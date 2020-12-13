package arikz.easyride.ui.main.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.Ride;
import arikz.easyride.models.UserInRide;
import arikz.easyride.util.ClusterMarker;
import arikz.easyride.util.GPSTracker;
import arikz.easyride.util.UserClusterManagerRenderer;
import arikz.easyride.util.UserMarkerManager;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MapFragment extends Fragment implements GoogleMap.OnPolylineClickListener, OnMapReadyCallback {
    private static final String TAG = ".MapFragment";
    private static final int LOCATION_REQUEST_CODE = 195;
    private GoogleMap mGoogleMap;
    private HashMap<String, Polyline> polyLines;
    private HashMap<String, Marker> pathInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Initialize view
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //Initialize map fragment
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        //Async map
        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(this);
        return view;
    }

    private void updateRidesRoutes() {
        String uid = getCurrentUserId();
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        if (uid != null) {
            dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        final String rid = snap.getValue(String.class);
                        if (rid != null) {
                            dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    List<UserInRide> participants = new ArrayList<>();
                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                        participants.add(snap.getValue(UserInRide.class));
                                    }
                                    createRideRoute(rid, participants);
                                    addUsersMarker(participants);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }


    }

    private void addUsersMarker(List<UserInRide> participants) {
        Context context = getContext();
        if (context != null) {
            if (mGoogleMap != null) {
                ClusterManager<ClusterMarker> clusterManager = new ClusterManager<>(context, mGoogleMap);
                UserClusterManagerRenderer clusterManagerRenderer = new UserClusterManagerRenderer(context, mGoogleMap, clusterManager);
                clusterManager.setRenderer(clusterManagerRenderer);
                UserMarkerManager userMarkerManager = new UserMarkerManager(clusterManager);
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    createRoute(src, dest, name, participants);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createRoute(String src, String dest, final String name, List<UserInRide> participants) {
        StringBuilder sb = new StringBuilder();
        int googleLimit = 8;
        for (UserInRide participant : participants) {
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
        sb.deleteCharAt(sb.length() - 1);
        String wayPoints = sb.toString();
        RequestQueue requestQueue = Volley.newRequestQueue(Objects.requireNonNull(getContext()));
        String URL = "https://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + src
                + "&destination=" + dest
                + "&mode=driving"
                + "&waypoints=" + wayPoints
                + "&alternatives=true"
                + "&key=" + getString(R.string.google_direction_api);
        Log.e("URL", URL);
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
                    addPolyline(route, name);
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

    private void addPolyline(List<List<HashMap<String, String>>> route, String name) {
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

            Context context = getContext();
            Polyline polyline = mGoogleMap.addPolyline(polylineOptions);
            polyLines.put(polyline.getId(), polyline);
            if (context != null) {
                polyline.setColor(context.getColor(R.color.colorGrey));
                polyline.setWidth(13);
                polyline.setPoints(points);
                polyline.setClickable(true);
                addPathInfo(polyline, name);
            }
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        Context context = getContext();
        if (context != null) {
            Polyline clickedPolyline = polyLines.get(polyline.getId());
            Marker clickedPathInfo = pathInfo.get(polyline.getId());
            if (clickedPolyline != null && clickedPathInfo != null) {
                clickedPolyline.setColor(context.getColor(R.color.colorAccent));
                clickedPolyline.setWidth(15);
                clickedPathInfo.setVisible(true);
                clickedPathInfo.showInfoWindow();
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clickedPathInfo.getPosition(),14));
                for (Map.Entry<String, Polyline> path : polyLines.entrySet()) {
                    Polyline line = polyLines.get(path.getKey());
                    if (line != null) {
                        if (!line.equals(clickedPolyline)) {
                            line.setColor(context.getColor(R.color.colorGrey));
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
    }

    private void addPathInfo(Polyline polyline, String name) {
        int middlePoint = polyline.getPoints().size() / 2;
        final LatLng latLng = polyline.getPoints().get(middlePoint);
        if (pathInfo == null) {
            pathInfo = new HashMap<>();
        }
        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(name)
                .snippet("more info"));
        marker.setVisible(false);
        pathInfo.put(polyline.getId(), marker);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        Context context = getContext();
        if (context != null) {
            if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                LocationManager locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
                //GPSTracker tracker = new GPSTracker(googleMap);
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, tracker);
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, tracker);
                }
            }
            updateRidesRoutes();
            mGoogleMap.setOnPolylineClickListener(this);
        }
    }
}
