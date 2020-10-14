package arikz.easyride.ui.main.rides;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
import arikz.easyride.ui.main.rides.add.AddRideActivity;
import arikz.easyride.ui.main.rides.add.adapters.RidesAdapter;

//TODO ADD DATE PICKER!
public class RidesFragment extends Fragment implements RidesAdapter.OnRideClicked {
    private static String TAG = ".RidesFragment";
    private static int ADD_REQUEST_CODE = 4;
    private static int LEAVE_REQUEST_CODE = 7;

    private View view;
    private ProgressBar pbRides;
    private RidesAdapter ridesAdapter;
    private List<Ride> rides;
    private long cntThreads, totalThread;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_rides, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        pbRides = view.findViewById(R.id.pbRides);
        ExtendedFloatingActionButton fabAddRide = view.findViewById(R.id.fabAddRide);
        RecyclerView rvRides = view.findViewById(R.id.rvRides);
        rvRides.setHasFixedSize(true);
        rvRides.setLayoutManager(new LinearLayoutManager(getContext()));

        rides = new ArrayList<>();
        ridesAdapter = new RidesAdapter(getContext(), this, rides);
        rvRides.setAdapter(ridesAdapter);

        collectRidesInfo();

        fabAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getArguments() != null) {
                    Intent intent = new Intent(getActivity(), AddRideActivity.class);
                    intent.putExtra("user", getArguments().getParcelable("user"));
                    startActivityForResult(intent, ADD_REQUEST_CODE);
                }
            }
        });

    }

    private void collectRidesInfo() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        final String uid = getCurrentUserId();
        if (uid != null) {
            dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        countThread();
                        countTotalThread(snapshot);
                        for (final DataSnapshot snap : snapshot.getChildren()) {
                            final String rid = snap.getValue(String.class);
                            if (rid != null) {
                                dbRef.child("rides").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        countThread();
                                        final Ride ride = snapshot.getValue(Ride.class);
                                        if (ride != null) {
                                            dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    countTotalThread(snapshot);
                                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                                        countThread();
                                                        UserInRide user = snap.getValue(UserInRide.class);
                                                        if (user != null) {
                                                            if (user.getUid().equals(uid) && user.isInRide()) {
                                                                rides.add(ride);
                                                                ridesAdapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                    }

                                                    if (allThreadFinished())
                                                        pbRides.setVisibility(View.INVISIBLE);
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e(TAG, error.getMessage());
                                                }
                                            });
                                        } else
                                            pbRides.setVisibility(View.INVISIBLE);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, error.getMessage());
                                    }
                                });
                            } else
                                pbRides.setVisibility(View.INVISIBLE);
                        }

                    } else
                        pbRides.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    private boolean allThreadFinished() {
        return cntThreads >= totalThread;
    }

    private synchronized void countTotalThread(DataSnapshot snapshot) {
        totalThread += snapshot.getChildrenCount();
    }

    private synchronized void countThread() {
        cntThreads++;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Ride ride = data.getExtras().getParcelable("ride");
                rides.add(ride);
                ridesAdapter.notifyDataSetChanged();
            }
        }

        if (requestCode == LEAVE_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                String rid = data.getExtras().getString("ride");

                if (!rides.isEmpty())
                    for (Ride ride : rides)
                        if (ride.getRid().equals(rid))
                            rides.remove(ride);

                ridesAdapter.notifyDataSetChanged();
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
    public void onClick(int index) {
        Intent intent = new Intent(getActivity(), RideInfoActivity.class);
        intent.putExtra("ride", rides.get(index));
        startActivityForResult(intent, LEAVE_REQUEST_CODE);
    }
}