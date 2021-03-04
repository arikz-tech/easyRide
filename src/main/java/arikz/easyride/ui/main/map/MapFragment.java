package arikz.easyride.ui.main.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.util.DistanceComparator;
import arikz.easyride.models.Ride;
import arikz.easyride.models.UserInRide;
import arikz.easyride.util.ClusterMarker;
import arikz.easyride.util.GPSMarker;
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
    private HashMap<String, List<LatLng>> pathBounds;
    private List<LatLng> srcPoints;
    private int numOfRides = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Initialize view
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        FloatingActionButton fabLocation = view.findViewById(R.id.fabLocation);

        //Initialize map fragment
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        //Async map
        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(this);

        fabLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context != null) {
                    if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
                    } else {
                        findLocation(context);
                    }
                }
            }

        });

        return view;
    }

    private void findLocation(Context context) {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
        GPSMarker tracker = new GPSMarker(context, mGoogleMap);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, tracker);
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, tracker);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findLocation(getContext());
            } else {
            }
        }
    }

    private void updateRidesRoutes() {
        final String uid = getCurrentUserId();
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        if (uid != null) {
            dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (final DataSnapshot snap : snapshot.getChildren()) {
                        increment();
                        final String rid = snap.getValue(String.class);
                        if (rid != null) {
                            dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    List<UserInRide> participants = new ArrayList<>();
                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                        participants.add(snap.getValue(UserInRide.class));
                                    }

                                    for (UserInRide participant : participants) {
                                        if (participant.getUid().equals(uid)) {
                                            if (participant.isInRide()) {
                                                createRideRoute(rid, participants);
                                                addUsersMarker(participants);
                                                double lat = Double.parseDouble(participant.getLatitude());
                                                double lng = Double.parseDouble(participant.getLongitude());
                                                LatLng srcPoint = new LatLng(lat, lng);
                                                addSourcePoint(srcPoint);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, error.getMessage());
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    private void addSourcePoint(LatLng src) {
        if (srcPoints == null) {
            srcPoints = new ArrayList<>();
        }
        srcPoints.add(src);
        decrement();
        if (isLastRide()) {
            LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
            for (LatLng srcPoint : srcPoints) {
                boundBuilder.include(srcPoint);
            }
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen
            LatLngBounds bounds = boundBuilder.build();
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
        }
    }

    private synchronized boolean isLastRide() {
        return numOfRides == 0;
    }

    private synchronized void decrement() {
        numOfRides--;
    }

    private synchronized void increment() {
        numOfRides++;
    }

    private void addUsersMarker(List<UserInRide> participants) {
        Context context = getContext();
        if (context != null) {
            if (mGoogleMap != null) {
                ClusterManager<ClusterMarker> clusterManager = new ClusterManager<>(context, mGoogleMap);
                UserClusterManagerRenderer clusterManagerRenderer = new UserClusterManagerRenderer(context, mGoogleMap, clusterManager);
                clusterManager.setRenderer(clusterManagerRenderer);
                UserMarkerManager userMarkerManager = new UserMarkerManager(context, clusterManager);
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
                    String date = ride.getDate();
                    Context context = getContext();
                    if (context != null) {
                        LatLng srcLatLng = getAddressLatLng(context, src);
                        if (srcLatLng != null) {
                            DistanceComparator comparator = new DistanceComparator(srcLatLng);
                            Collections.sort(participants, comparator);
                            createRoute(name, src, dest, date, participants);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createRoute(final String name, String src, String dest, final String date, final List<UserInRide> participants) {
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
        Context context = getContext();
        if (context != null) {
            RequestQueue requestQueue = Volley.newRequestQueue(context);
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
                        addPolyline(name, date, participants, route);
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

    private void addPolyline(String name, String date, List<UserInRide> participants, List<List<HashMap<String, String>>> route) {
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
                polyline.setColor(context.getColor(R.color.black));
                polyline.setWidth(13);
                polyline.setPoints(points);
                polyline.setClickable(true);
                addPathInfo(name, date, polyline);

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
                pathBounds.put(polyline.getId(), boundPoints);
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
            LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
            Polyline clickedPath = polyLines.get(polyline.getId());
            Marker clickedPathInfo = pathInfo.get(polyline.getId());
            if (clickedPath != null && clickedPathInfo != null) {
                clickedPath.setColor(context.getColor(R.color.amber_500));
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
                            line.setColor(context.getColor(R.color.black));
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

    private void addPathInfo(String name, String date, Polyline polyline) {
        LatLng latLng = polyline.getPoints().get(0);
        if (pathInfo == null) {
            pathInfo = new HashMap<>();
        }
        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
                .title(name)
                .snippet(getString(R.string.date_colon) + " " + date));
        marker.setVisible(false);
        pathInfo.put(polyline.getId(), marker);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.night_map_style));
        }
        updateRidesRoutes();
        mGoogleMap.setOnPolylineClickListener(this);
    }

    private LatLng getAddressLatLng(Context context, String address) {
        List<Address> addresses;

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

}
