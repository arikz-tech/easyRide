package arikz.easyride.ui.main.requests;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.UserInRide;

public class RequestsFragment extends Fragment implements RequestsAdapter.OnRequestClicked {
    private static String TAG = ".RequestsFragment";
    private static final int PERMISSION_REQUEST_CODE = 19;

    private View view;
    private ProgressBar pbRequests;
    private RequestsAdapter requestsAdapter;
    private List<Ride> requests;
    private int indexPar;//On permission result parameters

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_requests, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pbRequests = view.findViewById(R.id.pbRequests);
        RecyclerView rvRequests = view.findViewById(R.id.rvRequests);
        rvRequests.setHasFixedSize(true);
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        requests = new ArrayList<>();
        requestsAdapter = new RequestsAdapter(getActivity(), this, requests);
        rvRequests.setAdapter(requestsAdapter);
        collectRequestsInfo();
    }

    private void collectRequestsInfo() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        final String uid = getCurrentUserId();
        if (uid != null) {
            dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (final DataSnapshot snap : snapshot.getChildren()) {
                            final String rid = snap.getValue(String.class);
                            if (rid != null) {
                                dbRef.child("rides").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        final Ride ride = snapshot.getValue(Ride.class);
                                        if (ride != null) {
                                            dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                                        UserInRide user = snap.getValue(UserInRide.class);
                                                        if (user.getUid().equals(uid) && !user.isInRide()) {
                                                            requests.add(ride);
                                                            requestsAdapter.notifyDataSetChanged();
                                                        }

                                                        int lastUser = Integer.parseInt(snap.getKey());
                                                        if (lastUser == snapshot.getChildrenCount() - 1){
                                                            try {
                                                                Thread.sleep((long) 0.1);
                                                            } catch (InterruptedException e) {
                                                                e.printStackTrace();
                                                            }
                                                            pbRequests.setVisibility(View.INVISIBLE);
                                                        }

                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e(TAG, error.getMessage());
                                                }
                                            });
                                        } else
                                            pbRequests.setVisibility(View.INVISIBLE);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, error.getMessage());
                                    }
                                });
                            } else
                                pbRequests.setVisibility(View.INVISIBLE);
                        }
                    } else
                        pbRequests.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        } else
            pbRequests.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClick(indexPar, true);
            } else {
                requestsAdapter.viewHolder.changeState(false);
                Toast.makeText(getContext(), "To let the ride owner know where to pick you up you have to gran permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(int index, final boolean confirm) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Save function parameters
            indexPar = index;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 500.0f, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                }
            });
            final Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            final String rid = requests.get(index).getRid();
            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
            dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String uid = getCurrentUserId();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        UserInRide user = snap.getValue(UserInRide.class);
                        if (user.getUid().equals(uid)) {
                            String key = snap.getKey();
                            dbRef.child("rideUsers").child(rid).child(key).child("inRide").setValue(confirm);
                            dbRef.child("rideUsers").child(rid).child(key).child("latitude").setValue(location.getLatitude());
                            dbRef.child("rideUsers").child(rid).child(key).child("longitude").setValue(location.getLongitude());
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

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }
}