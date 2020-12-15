package arikz.easyride.ui.main.requests;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.adapters.RequestsAdapter;
import arikz.easyride.models.Ride;
import arikz.easyride.models.UserInRide;
import arikz.easyride.util.LoadData;

public class RequestsFragment extends Fragment implements RequestsAdapter.OnRequestClicked {
    private static String TAG = ".RequestsFragment";
    private static final int LOCATION_REQUEST_CODE = 59;

    private View view;
    private ImageView ivNoRequests;
    private MaterialTextView tvNoRequests;
    private ProgressBar pbRequests, progressBarPar;
    private RequestsAdapter requestsAdapter;
    private List<Ride> requests;
    private LocationManager locationManager;
    private int indexPar;  //On permission result parameters
    private MaterialButton buttonPar; //On permission result parameters
    private boolean loading = false;
    private boolean[] confirmed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_requests, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ivNoRequests = view.findViewById(R.id.ivNoRequests);
        tvNoRequests = view.findViewById(R.id.tvNoRequests);
        pbRequests = view.findViewById(R.id.pbRequests);
        RecyclerView rvRequests = view.findViewById(R.id.rvRequests);
        rvRequests.setHasFixedSize(true);
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        requests = new ArrayList<>();
        requestsAdapter = new RequestsAdapter(getActivity(), this, requests);
        rvRequests.setAdapter(requestsAdapter);

        LoadData loadRequests = new LoadData();
        loadRequests.setItemsList(requests);
        loadRequests.setRequestsAdapter(requestsAdapter);
        loadRequests.setProgressBar(pbRequests);
        loadRequests.setTvNoData(tvNoRequests);
        loadRequests.setIvNoData(ivNoRequests);
        loadRequests.load();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserCurrentPosition();
                displayProgressBar();
            } else {
                Toast.makeText(getContext(), "To let the ride owner know where to pick you up you have to grant permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(int index, MaterialButton button, ProgressBar progressBar) {
        if (!loading) {
            indexPar = index;
            buttonPar = button;
            progressBarPar = progressBar;

            if (confirmed == null) {
                confirmed = new boolean[requests.size()];
            }

            if (!confirmed[index]) {
                //dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.pickup_location);
                builder.setMessage(R.string.wich_location);
                builder.setIcon(R.drawable.ic_address_24);
                builder.setPositiveButton(R.string.current_location, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayProgressBar();
                        getUserCurrentPosition();
                    }
                }).setNegativeButton(R.string.saved_location, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayProgressBar();
                        getUserSavedAddress();
                    }
                }).show();
            }
        } else {
            Toast.makeText(getContext(), R.string.loading, Toast.LENGTH_SHORT).show();
        }
    }

    private synchronized void displayProgressBar() {
        loading = true;
        /*Show progress bar and hide button*/
        progressBarPar.setVisibility(View.VISIBLE);
        buttonPar.setVisibility(View.INVISIBLE);
    }

    private synchronized void hideProgressBar() {
        /*Show progress bar and hid button*/
        progressBarPar.setVisibility(View.INVISIBLE);
        buttonPar.setVisibility(View.VISIBLE);
        loading = false;
    }

    private synchronized void buttonChangeToConfirmed() {
        /*Change button style, display confirmed button*/
        buttonPar.setStrokeColorResource(R.color.colorPrimary);
        buttonPar.setTextColor(Objects.requireNonNull(getActivity()).getColor(R.color.colorPrimary));
        buttonPar.setText(R.string.confirmed);
    }

    private synchronized void buttonChangeToConfirm() {
        /*Change button style, display confirmed button*/
        buttonPar.setStrokeColorResource(R.color.colorBlack);
        buttonPar.setTextColor(Objects.requireNonNull(getActivity()).getColor(R.color.colorBlack));
        buttonPar.setText(R.string.confirm);
    }

    private synchronized void getUserSavedAddress() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String uid = getCurrentUserId();
        class AddressListener implements ValueEventListener {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String address = snapshot.getValue(String.class);
                if (address == null) {
                    Toast.makeText(getContext(), R.string.add_address_location, Toast.LENGTH_SHORT).show();
                    buttonChangeToConfirm();
                } else {
                    final LatLng location = getAddressLatLng(address);
                    if (location == null) {
                        Toast.makeText(getContext(), R.string.add_address_location, Toast.LENGTH_SHORT).show();
                        buttonChangeToConfirm();
                    } else {
                        final String rid = requests.get(indexPar).getRid();
                        dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String uid = getCurrentUserId();
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    UserInRide user = snap.getValue(UserInRide.class);
                                    if (Objects.requireNonNull(user).getUid().equals(uid)) {
                                        String key = snap.getKey();
                                        dbRef.child("rideUsers").child(rid).child(Objects.requireNonNull(key)).child("inRide").setValue(true);
                                        dbRef.child("rideUsers").child(rid).child(key).child("latitude").setValue(location.latitude + "");
                                        dbRef.child("rideUsers").child(rid).child(key).child("longitude").setValue(location.longitude + "");

                                        confirmed[indexPar] = true;
                                        buttonChangeToConfirmed();
                                        hideProgressBar();
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }

        }
        AddressListener addressListener = new AddressListener();
        if (uid != null) {
            dbRef.child("users").child(uid).child("address").addListenerForSingleValueEvent(addressListener);
        }
    }

    private void getUserCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            buttonChangeToConfirm();
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            class Listener implements LocationListener {
                @Override
                public void onLocationChanged(@NonNull final Location location) {
                    final String rid = requests.get(indexPar).getRid();
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
                                    dbRef.child("rideUsers").child(rid).child(key).child("latitude").setValue(location.getLatitude() + "");
                                    dbRef.child("rideUsers").child(rid).child(key).child("longitude").setValue(location.getLongitude() + "");

                                    confirmed[indexPar] = true;
                                    buttonChangeToConfirmed();
                                    hideProgressBar();
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
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
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

    private LatLng getAddressLatLng(String address) {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            double lat = addresses.get(0).getLatitude();
            double lng = addresses.get(0).getLongitude();
            return new LatLng(lat, lng);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}