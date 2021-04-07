package arik.easyride.ui.main.map;

import android.Manifest;
import android.app.Activity;
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

import arik.easyride.R;
import arik.easyride.ui.main.rides.info.MapActivity;
import arik.easyride.util.DistanceComparator;
import arik.easyride.models.Ride;
import arik.easyride.models.UserInRide;
import arik.easyride.util.ClusterMarker;
import arik.easyride.util.GPSMarker;
import arik.easyride.util.RideDirections;
import arik.easyride.util.UserClusterManagerRenderer;
import arik.easyride.util.UserMarkerManager;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private static final String TAG = ".MapFragment";
    private static final int LOCATION_REQUEST_CODE = 195;
    private GoogleMap mGoogleMap;
    private List<List<LatLng>> allRideBoundaries;
    private long numberOfRides;
    private HashMap<String, RideDirections> listOfDirections;
    private final Context context = getContext();
    private final Activity activity = getActivity();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Initialize view
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        FloatingActionButton fabLocation = view.findViewById(R.id.fabLocation);

        //Initialize map fragment
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if (supportMapFragment != null) {
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
        }
        return view;
    }

    private void findLocation(Context context) {
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
        GPSMarker tracker = new GPSMarker(context, mGoogleMap);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                findLocation(context);
            }
        }
    }

    private void addUsersMarker(List<UserInRide> participants) {
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

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (context != null) {
            int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.night_map_style));
            }
        }
        setAllRidesDirections();
        mGoogleMap.setOnPolylineClickListener(this);
    }

    private void setAllRidesDirections() {
        final String uid = getCurrentUserId();
        allRideBoundaries = new ArrayList<>();
        listOfDirections = new HashMap<>();
        if(uid != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("userRides");
            dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (activity != null) {
                        synchronized (activity) {
                            numberOfRides = snapshot.getChildrenCount();
                        }
                    }

                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String rid = snap.getValue(String.class);
                        checkIfUserExistInRideAndAddDirections(uid, rid);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }

            });
        }
    }

    private void checkIfUserExistInRideAndAddDirections(final String uid, final String rid) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("rideUsers");
        dbRef.child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    UserInRide user = snap.getValue(UserInRide.class);
                    if (user != null) {
                        if (user.getUid().equals(uid)) {
                            if (user.isInRide()) {
                                setRideDirections(rid);
                            } else {
                                if (activity != null) {
                                    synchronized (activity) {
                                        numberOfRides--;
                                    }
                                }
                            }
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

    private void setRideDirections(final String rid) {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserInRide> participants = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    participants.add(snap.getValue(UserInRide.class));
                }

                createRideDirections(rid, participants);
                addUsersMarker(participants);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void createRideDirections(String rid, final List<UserInRide> participants) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rides").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Ride ride = snapshot.getValue(Ride.class);
                if (ride != null) {
                    if (activity != null) {
                        if (context != null) {
                            RideDirections directions = new RideDirections(getContext(), mGoogleMap, ride, participants);
                            directions.setDefaultPolylineColor(context.getColor(R.color.black));
                            directions.setClickedPolylineColor(context.getColor(R.color.deep_orange_500));
                            directions.createRoute();
                            directions.setPolylineBoundaries();
                            allRideBoundaries.add(directions.getPolylineBoundaries());
                            listOfDirections.put(ride.getRid(), directions);

                            synchronized (activity) {
                                numberOfRides--;
                                if (numberOfRides == 0) {
                                    moveCameraToAllPolylinePosition();
                                    mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                                        @Override
                                        public void onMapClick(LatLng latLng) {
                                            for (Map.Entry<String, RideDirections> directions : listOfDirections.entrySet()) {
                                                RideDirections rideDirections = directions.getValue();
                                                rideDirections.clearMarkedPolyline();
                                            }
                                        }
                                    });
                                }
                            }
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

    public void moveCameraToAllPolylinePosition() {
        LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();

        for (List<LatLng> polylineBoundaries : allRideBoundaries) {
            for (LatLng point : polylineBoundaries) {
                boundBuilder.include(point);
            }
        }

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen
        LatLngBounds bounds = boundBuilder.build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        for (Map.Entry<String, RideDirections> directions : listOfDirections.entrySet()) {
            RideDirections rideDirections = directions.getValue();
            String polylineID = rideDirections.getPolyline().getId();
            if (polylineID.equals(polyline.getId())) {
                rideDirections.onPolylineClick();
            } else {
                rideDirections.clearMarkedPolyline();
            }
        }
    }
}
