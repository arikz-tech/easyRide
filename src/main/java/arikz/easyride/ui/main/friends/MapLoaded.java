package arikz.easyride.ui.main.friends;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import arikz.easyride.models.User;
import arikz.easyride.util.ClusterMarker;
import arikz.easyride.util.UserClusterManagerRenderer;

public class MapLoaded implements OnMapReadyCallback {

    private static final String TAG = ".MapLoaded";

    private GoogleMap mGoogleMap;
    private User currentUser;
    private Context context;
    private ClusterManager<ClusterMarker> clusterManager;
    private UserClusterManagerRenderer clusterManagerRenderer;

    MapLoaded(Context context, User currentUser) {
        this.currentUser = currentUser;
        this.context = context;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        List<Address> addresses;
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            if (currentUser.getAddress() != null) {
                addresses = geocoder.getFromLocationName(currentUser.getAddress(), 1);
                LatLng latLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                googleMap.setMinZoomPreference(12);
                addCluster(latLng);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }





    }

    private void addCluster(final LatLng clusterLatLng) {
        if (clusterManager == null) {
            clusterManager = new ClusterManager<>(context, mGoogleMap);
        }
        if (clusterManagerRenderer == null) {
            clusterManagerRenderer = new UserClusterManagerRenderer(context, mGoogleMap, clusterManager);
            clusterManager.setRenderer(clusterManagerRenderer);
        }

        String pid = currentUser.getPid();
        if (pid != null) {
            Task<byte[]> task = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid).getBytes(Long.MAX_VALUE);
            task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(final byte[] bytes) {
                    FirebaseDatabase.getInstance().getReference().
                            child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User userInfo = snapshot.getValue(User.class);
                            String markerName = Objects.requireNonNull(userInfo).displayName();
                            ClusterMarker check = new ClusterMarker(clusterLatLng, markerName, currentUser.getAddress(), bytes);
                            clusterManager.addItem(check);
                            clusterManager.cluster();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, error.getMessage());
                        }

                    });
                }
            });
        } else {
            FirebaseDatabase.getInstance().getReference().
                    child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User userInfo = snapshot.getValue(User.class);
                    String markerName = Objects.requireNonNull(userInfo).displayName();
                    ClusterMarker check = new ClusterMarker(clusterLatLng, markerName, currentUser.getAddress(), null);
                    clusterManager.addItem(check);
                    clusterManager.cluster();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }

            });
        }

    }
}
