package arik.easyride.ui.main.rides.info;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import arik.easyride.R;
import arik.easyride.util.DistanceComparator;
import arik.easyride.models.Ride;
import arik.easyride.models.User;
import arik.easyride.models.UserInRide;
import arik.easyride.ui.main.friends.FriendsInfoActivity;
import arik.easyride.adapters.ParticipantsAdapter;
import arik.easyride.util.Navigate;

public class RideInfoActivity extends AppCompatActivity implements ParticipantsAdapter.OnParticipantClick {
    private static final String TAG = ".RideInfoActivity";
    private static final int SMS_SENT_REQUEST_CODE = 12;

    private ProgressBar pbRideInfo;
    private FloatingActionButton fabMap, fabRoute;
    private ImageView ivRidePic;
    private MaterialButton btnDelete;
    private Ride ride;
    private ParticipantsAdapter participantsAdapter;
    private List<UserInRide> participants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_info);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.toolbarLayout);
        MaterialTextView tvSrcFill = findViewById(R.id.tvSrcFill);
        MaterialTextView tvDestFill = findViewById(R.id.tvDestFill);
        MaterialTextView tvDateFill = findViewById(R.id.tvDateFill);
        MaterialTextView tvTimeFill = findViewById(R.id.tvTimeFill);
        btnDelete = findViewById(R.id.btnDelete);
        ivRidePic = findViewById(R.id.ivRidePic);
        pbRideInfo = findViewById(R.id.pbRideInfo);
        fabMap = findViewById(R.id.fabMap);
        fabRoute = findViewById(R.id.fabRoute);

        RecyclerView rvParticipants = findViewById(R.id.rvParticipants);
        participants = new ArrayList<>();
        ride = getIntent().getExtras().getParcelable("ride");
        participantsAdapter = new ParticipantsAdapter(ride, participants, RideInfoActivity.this);
        rvParticipants.setAdapter(participantsAdapter);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(toolbar);
        assert getIntent().getExtras() != null;
        boolean fromRequest = getIntent().getBooleanExtra("fromRequestFrag", false);

        if (fromRequest)
            btnDelete.setVisibility(View.GONE);

        toolbarLayout.setTitle(Objects.requireNonNull(ride).getName());
        setRideImage();

        tvSrcFill.setText(ride.getSource());
        tvDestFill.setText(ride.getDestination());
        tvDateFill.setText(ride.getDate());
        tvTimeFill.setText(ride.getTime());

        collectParticipants();

        if (isOwner())
            btnDelete.setText(R.string.delete_ride);
        else
            btnDelete.setText(R.string.leave_ride);

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOwner())
                    deleteRideOwner();
                else
                    exitRide();
            }
        });

        fabRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String source = ride.getSource();
                String destination = ride.getDestination();
                LatLng src = getAddressLatLng(source);
                LatLng dest = getAddressLatLng(destination);

                if (src != null && dest != null) {
                    DistanceComparator comparator = new DistanceComparator(src);
                    Collections.sort(participants, comparator);

                    String url = "https://maps.google.com/maps?";
                    StringBuilder sb = new StringBuilder(url);
                    sb.append("saddr=");
                    sb.append(src.latitude);
                    sb.append(",");
                    sb.append(src.longitude);

                    boolean flag = false;
                    for (UserInRide participant : participants) {
                        if (participant.isInRide()) {
                            if (!flag) {
                                sb.append("&daddr=");
                                sb.append(participant.getLatitude());
                                sb.append(",");
                                sb.append(participant.getLongitude());
                                flag = true;
                            } else {
                                sb.append("+to:");
                                sb.append(participant.getLatitude());
                                sb.append(",");
                                sb.append(participant.getLongitude());
                            }
                        }
                    }

                    sb.append("+to:");
                    sb.append(dest.latitude);
                    sb.append(",");
                    sb.append(dest.longitude);

                    Uri uri = Uri.parse(sb.toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                } else {
                    Toast.makeText(RideInfoActivity.this, R.string.could_not_find_location, Toast.LENGTH_SHORT).show();
                }

            }

        });

        fabMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RideInfoActivity.this, MapActivity.class);
                intent.putExtra("ride", ride);
                startActivity(intent);
            }
        });

    }

    private LatLng getAddressLatLng(String address) {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            if (addresses.isEmpty())
                return null;
            double lat = addresses.get(0).getLatitude();
            double lng = addresses.get(0).getLongitude();
            return new LatLng(lat, lng);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void exitRide() {
        pbRideInfo.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.GONE);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        final String uid = getCurrentUserId();
        dbRef.child("userRides").child(Objects.requireNonNull(uid)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String key = snap.getKey();
                    String rid = snap.getValue(String.class);
                    if (Objects.equals(rid, ride.getRid()))
                        dbRef.child("userRides").child(uid).child(Objects.requireNonNull(key)).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Intent data = new Intent();
                                data.putExtra("ride", ride.getRid());
                                setResult(RESULT_OK, data);
                                finish();
                            }
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });

        dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String key = snap.getKey();
                    UserInRide user = snap.getValue(UserInRide.class);
                    if (Objects.requireNonNull(user).getUid().equals(uid))
                        dbRef.child("rideUsers").child(ride.getRid()).child(Objects.requireNonNull(key)).child("inRide").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void deleteRideOwner() {
        pbRideInfo.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.GONE);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        for (UserInRide participant : participants) {
            dbRef.child("users").child(participant.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (user.getEmail() == null) {
                            String key = snapshot.getKey();
                            if (key != null) {
                                dbRef.child("users").child(key).removeValue();
                            }
                            String pid = user.getPid();
                            if (pid != null) {
                                if (!pid.equals("avatar_logo.png")) {
                                    FirebaseStorage.getInstance().getReference().
                                            child("images").child("users").child(user.getPid()).delete();
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }

        dbRef.child("rides").child(ride.getRid()).removeValue();
        dbRef.child("rideUsers").child(ride.getRid()).removeValue();
        dbRef.child("userRides").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    final String uid = snap.getKey();
                    dbRef.child("userRides").child(Objects.requireNonNull(uid)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String key = snap.getKey();
                                String rid = snap.getValue(String.class);
                                if (Objects.equals(rid, ride.getRid()))
                                    dbRef.child("userRides").child(uid).child(Objects.requireNonNull(key)).removeValue();

                                if (Integer.parseInt(Objects.requireNonNull(key)) == snapshot.getChildrenCount() - 1) {
                                    try {
                                        Thread.sleep((long) 0.1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    Intent data = new Intent();
                                    data.putExtra("ride", ride.getRid());
                                    setResult(RESULT_OK, data);
                                    finish();
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });

        if (ride.getPid() != null)
            FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(ride.getPid()).delete();

    }

    private boolean isOwner() {
        return ride.getOwnerUID().equals(getCurrentUserId());
    }

    private void collectParticipants() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    UserInRide user = snap.getValue(UserInRide.class);
                    if (Objects.requireNonNull(user).isInRide()) {
                        participants.add(0, user); //Add participant into front of array list
                    } else {
                        participants.add(user);
                    }
                    participantsAdapter.notifyDataSetChanged();
                }
                pbRideInfo.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void setRideImage() {
        if (ride.getPid() != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(ride.getPid());

            Glide.with(this).load(imageRef).into(ivRidePic);
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
        final UserInRide userInRide = participants.get(index);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("users").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (user.getEmail() != null) {
                        Intent intent = new Intent(getApplicationContext(), FriendsInfoActivity.class);
                        intent.putExtra("userInfo", user);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private ImageView displayImageMap() {
        String latEiffelTower = "48.858235";
        String lngEiffelTower = "2.294571";
        String url = "http://maps.google.com/maps/api/staticmap?center="
                + latEiffelTower
                + ","
                + lngEiffelTower
                + "&zoom=15&size=200x200&sensor=false&key=YOUR_API_KEY";
        return null;
    }

}