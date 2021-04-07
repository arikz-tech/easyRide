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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import arik.easyride.R;
import arik.easyride.models.Ride;
import arik.easyride.models.UserInRide;
import arik.easyride.util.ClusterMarker;
import arik.easyride.util.RideDirections;
import arik.easyride.util.UserClusterManagerRenderer;
import arik.easyride.util.UserMarkerManager;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {
    private static final String TAG = ".MapActivity";
    private GoogleMap mGoogleMap;
    private Ride ride;
    private RideDirections directions;

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

        setRideDirections();
    }

    private void setRideDirections() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserInRide> participants = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    participants.add(snap.getValue(UserInRide.class));
                }

                createRideDirections(ride.getRid(), participants);
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

                    directions = new RideDirections(getApplicationContext(), mGoogleMap, ride, participants);
                    directions.setDefaultPolylineColor(getColor(R.color.black));
                    directions.setClickedPolylineColor(getColor(R.color.deep_orange_500));
                    directions.setPolylineBoundaries();
                    directions.setDisplayDirectionImmediately(true);
                    directions.createRoute();

                    mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            directions.clearMarkedPolyline();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
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

    @Override
    public void onPolylineClick(Polyline polyline) {
        directions.onPolylineClick();
    }
}