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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.UserInRide;
import arikz.easyride.ui.main.LoadData;

public class RequestsFragment extends Fragment implements RequestsAdapter.OnRequestClicked {
    private static String TAG = ".RequestsFragment";
    private static final int LOCATION_REQUEST_CODE = 59;

    private View view;
    private ProgressBar pbRequests, progressBarPar;
    private RequestsAdapter requestsAdapter;
    private List<Ride> requests;
    private LocationManager locationManager;
    private int indexPar;  //On permission result parameters
    private MaterialButton buttonPar; //On permission result parameters

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

        LoadData loadRequests = new LoadData(requests, null, requestsAdapter, pbRequests);
        loadRequests.load();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClick(indexPar, buttonPar, progressBarPar);
            } else {
                Toast.makeText(getContext(), "To let the ride owner know where to pick you up you have to gran permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(final int index, final MaterialButton button, final ProgressBar progressBar) {

        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Save function parameters
            indexPar = index;
            buttonPar = button;
            progressBarPar = progressBar;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            class Listener implements LocationListener {
                @Override
                public void onLocationChanged(@NonNull final Location location) {
                    final String rid = requests.get(index).getRid();
                    final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    locationManager.removeUpdates(this);
                    dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String uid = getCurrentUserId();
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                UserInRide user = snap.getValue(UserInRide.class);
                                if (Objects.requireNonNull(user).getUid().equals(uid)) {
                                    String key = snap.getKey();
                                    dbRef.child("rideUsers").child(rid).child(Objects.requireNonNull(key)).child("inRide").setValue(true);
                                    dbRef.child("rideUsers").child(rid).child(key).child("latitude").setValue(Objects.requireNonNull(location).getLatitude());
                                    dbRef.child("rideUsers").child(rid).child(key).child("longitude").setValue(location.getLongitude());
                                    Toast.makeText(getActivity(), R.string.accept_ride, Toast.LENGTH_SHORT).show();

                                    /*Display confirmed button*/
                                    button.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.INVISIBLE);
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
            Listener listener = new Listener();
            locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5, listener);

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