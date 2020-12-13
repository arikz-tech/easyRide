package arikz.easyride.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.maps.android.clustering.ClusterManager;

import arikz.easyride.models.User;
import arikz.easyride.models.UserInRide;

public class UserMarkerManager {
    private static final String TAG = ".UserMarkerManager";

    private ClusterManager<ClusterMarker> clusterManager;

    public UserMarkerManager(ClusterManager<ClusterMarker> clusterManager){
        this.clusterManager  = clusterManager;
    }


    public void addToMap(final UserInRide userInRide) {
        FirebaseDatabase.getInstance().getReference().
                child("users").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final User user = snapshot.getValue(User.class);
                if (user != null) {
                    Task<byte[]> task = FirebaseStorage.getInstance().getReference().
                            child("images").child("users").child(user.getPid()).getBytes(Long.MAX_VALUE);
                    task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(final byte[] bytes) {
                            addCluster(userInRide, bytes);
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

    private void addCluster(final UserInRide userInRide, final byte[] imageByteArray) {
        FirebaseDatabase.getInstance().getReference().
                child("users").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double lat = Double.parseDouble(userInRide.getLatitude());
                double lng = Double.parseDouble(userInRide.getLongitude());
                User userInfo = snapshot.getValue(User.class);
                LatLng latLng = new LatLng(lat, lng);
                String address = getAddressFromLatLng(latLng);
                if (userInfo != null) {
                    String markerName = userInfo.displayName();
                    ClusterMarker newCluster = new ClusterMarker(latLng, markerName, address, imageByteArray);
                    clusterManager.addItem(newCluster);
                    clusterManager.cluster();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });

    }

    private String getAddressFromLatLng(LatLng latLng) {
        return "";
    }
}
