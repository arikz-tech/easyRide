package arikz.easyride.ui.main.rides.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import arikz.easyride.R;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;
import arikz.easyride.ui.main.GlideOptions;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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
        double avgLat = 0, avgLong = 0;
        int numOfMarkers = 0;

        List<UserInRide> users = getIntent().getParcelableArrayListExtra("users");

        for (UserInRide user : users) {
            if(user.isInRide()){
                numOfMarkers++;
            MarkerOptions markerOptions = new MarkerOptions();
            avgLat += user.getLatitude();
            avgLong += user.getLongitude();
            LatLng position = new LatLng(user.getLatitude(), user.getLongitude());
            markerOptions.position(position);
            markerOptions.title(user.getUid());
            googleMap.addMarker(markerOptions);
            }
        }

        LatLng avgPosition = new LatLng(avgLat / numOfMarkers, avgLong / numOfMarkers);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(avgPosition, 10));
    }

}