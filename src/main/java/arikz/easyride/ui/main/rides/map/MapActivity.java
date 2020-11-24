package arikz.easyride.ui.main.rides.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.GeocodedWaypoint;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;
import arikz.easyride.ui.main.GlideOptions;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = ".MapActivity";
    private ClusterManager<ClusterMarker> clusterManager;
    private MyClusterManagerRenderer clusterManagerRenderer;
    private GoogleMap mGoogleMap;
    private ArrayList<byte[]> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);
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
        mGoogleMap = googleMap;
        List<UserInRide> users = getIntent().getParcelableArrayListExtra("users");

        if (googleMap != null) {
            if (clusterManager == null) {
                clusterManager = new ClusterManager<>(getApplicationContext(), googleMap);
            }
            if (clusterManagerRenderer == null) {
                clusterManagerRenderer = new MyClusterManagerRenderer(getApplicationContext(), googleMap, clusterManager);
                clusterManager.setRenderer(clusterManagerRenderer);
            }

            List<LatLng> latLngPoints = new ArrayList<>();
            LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
            if (users != null) {
                for (UserInRide user : users) {
                    if (user.isInRide()) {
                        LatLng newPoint = new LatLng(user.getLatitude(), user.getLongitude());
                        latLngPoints.add(newPoint);
                        boundBuilder.include(newPoint);
                        addCluster(user);
                    }
                }
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen
                LatLngBounds bounds = boundBuilder.build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
            }

            LatLng from = latLngPoints.get(0);
            LatLng to = new LatLng(31.603970, 34.766240);

            //requestDirection(from, to, latLngPoints); //CHECK API BILLING !:o
        }
    }

    private void addCluster(final UserInRide userInRide) {
        FirebaseDatabase.getInstance().getReference().
                child("users").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final User user = snapshot.getValue(User.class);
                if (Objects.requireNonNull(user).getPid() != null) {
                    Task<byte[]> task = FirebaseStorage.getInstance().getReference().
                            child("images").child("users").child(user.getPid()).getBytes(Long.MAX_VALUE);
                    task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(final byte[] bytes) {
                            FirebaseDatabase.getInstance().getReference().
                                    child("users").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    List<Address> addresses;
                                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                                    try {
                                        addresses = geocoder.getFromLocation(userInRide.getLatitude(), userInRide.getLongitude(), 1);
                                        if (!addresses.isEmpty()) {
                                            User userInfo = snapshot.getValue(User.class);
                                            LatLng latLng = new LatLng(userInRide.getLatitude(), userInRide.getLongitude());
                                            String markerName = Objects.requireNonNull(userInfo).displayName();
                                            String addressName = addresses.get(0).getAddressLine(0);
                                            ClusterMarker check = new ClusterMarker(latLng, markerName, addressName, bytes);
                                            clusterManager.addItem(check);
                                            clusterManager.cluster();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, error.getMessage());
                                }
                            });

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

    public void requestDirection(LatLng from, LatLng to, List<LatLng> latLngPoints) {
        GoogleDirection.withServerKey(getString(R.string.google_direction_api))
                .from(from)
                .and(latLngPoints)
                .to(to)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(@Nullable Direction direction) {
                        if (direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            ArrayList<LatLng> pointsList = leg.getDirectionPoint();
                            PolylineOptions polylineOptions = DirectionConverter
                                    .createPolyline(getApplicationContext(), pointsList, 3, getColor(R.color.colorAccent));
                            mGoogleMap.addPolyline(polylineOptions);
                            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(pointsList.get(0)));
                        }
                    }

                    @Override
                    public void onDirectionFailure(@NonNull Throwable t) {
                        Log.e(TAG, t.toString());
                    }
                });
    }

}

