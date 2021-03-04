package arikz.easyride.ui.main.rides.info;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.maps.GeoApiContext;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;
import arikz.easyride.models.UserInRide;
import arikz.easyride.util.ClusterMarker;
import arikz.easyride.util.UserClusterManagerRenderer;
import arikz.easyride.util.UserMarkerManager;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = ".MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        List<UserInRide> users = getIntent().getParcelableArrayListExtra("users");

        if (googleMap != null) {
            int nightModeFlags = getBaseContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getBaseContext(), R.raw.night_map_style));
            }
            ClusterManager<ClusterMarker> clusterManager = new ClusterManager<>(getApplicationContext(), googleMap);
            UserClusterManagerRenderer clusterManagerRenderer = new UserClusterManagerRenderer(getApplicationContext(), googleMap, clusterManager);
            clusterManager.setRenderer(clusterManagerRenderer);
            UserMarkerManager userMarkerManager = new UserMarkerManager(getApplicationContext(), clusterManager);
            if (users != null) {
                LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
                for (UserInRide user : users) {
                    if (user.isInRide()) {
                        double lat = Double.parseDouble(user.getLatitude());
                        double lng = Double.parseDouble(user.getLongitude());
                        LatLng newPoint = new LatLng(lat, lng);
                        boundBuilder.include(newPoint);
                        userMarkerManager.addToMap(user);
                    }
                }
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen
                LatLngBounds bounds = boundBuilder.build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
            }

        }
    }


}