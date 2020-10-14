package arikz.easyride.ui.main.requests;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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

public class RequestsFragment extends Fragment {
    private static String TAG = ".RequestsFragment";
    private View view;
    private ProgressBar pbRequests;
    private RequestsAdapter requestsAdapter;
    private List<Ride> requests;
    private long cntThreads, totalThread;

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
        requestsAdapter = new RequestsAdapter(getContext(), requests);
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
                                                        if (user.getUid().equals(uid) && !user.isInRide()) {
                                                            requests.add(ride);
                                                            requestsAdapter.notifyDataSetChanged();
                                                        }

                                                        if (allThreadFinished())
                                                            pbRequests.setVisibility(View.INVISIBLE);
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
        }
    }

    private boolean allThreadFinished() {
        return cntThreads == totalThread;
    }

    private synchronized void countTotalThread(DataSnapshot snapshot) {
        totalThread += snapshot.getChildrenCount();
    }

    private synchronized void countThread() {
        cntThreads++;
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }
}