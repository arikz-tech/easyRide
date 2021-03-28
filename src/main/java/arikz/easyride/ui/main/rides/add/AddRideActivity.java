package arikz.easyride.ui.main.rides.add;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import arikz.easyride.R;
import arikz.easyride.models.Ride;
import arikz.easyride.models.User;
import arikz.easyride.models.UserInRide;
import arikz.easyride.ui.main.rides.add.interfaces.ParticipantsEvents;
import arikz.easyride.ui.main.rides.add.interfaces.DetailsEvents;
import arikz.easyride.ui.main.rides.add.tabs.DetailsFragment;
import arikz.easyride.ui.main.rides.add.tabs.ParticipantsFragment;

public class AddRideActivity extends AppCompatActivity implements ParticipantsEvents, DetailsEvents {
    private static final String TAG = ".AddRideActivity";
    private static final int SMS_SENT_REQUEST_CODE = 14;

    private ViewPager2 viewPager;
    private List<User> rideParticipants;
    private User currentUser;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ride);

        rideParticipants = new ArrayList<>();
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        getCurrentUser();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        ViewPager2Adapter viewPager2Adapter = new ViewPager2Adapter(this);
        viewPager.setAdapter(viewPager2Adapter);
        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0:
                        tab.setText(getText(R.string.ride_details));
                        tab.setIcon(R.drawable.ic_rides_24);
                        break;
                    case 1:
                        tab.setText(getText(R.string.participants));
                        tab.setIcon(R.drawable.ic_friends_24);
                        break;
                }
            }
        }).attach();
    }

    private void getCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        assert user != null;
        dbRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    @Override
    public void onAdd(User participant) {
        rideParticipants.add(participant);
    }

    private void uploadImageDatabase(User participant) {
        String pid = UUID.randomUUID().toString();
        if (participant.getPid() != null) {
            FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid).putFile(Uri.parse(participant.getPid()));
            participant.setPid(pid);
        } else {
            participant.setPid("avatar_logo.png");
        }
    }

    @Override
    public void onRemove(User participant) {
        rideParticipants.remove(participant);
    }

    @Override
    public void onImageUpload() {
        saving = true;
    }

    @Override
    public void onClickAddParticipants() {
        viewPager.setCurrentItem(1, true);
    }

    @Override
    public void onSubmit(String name, String src, String dest, String date, String time, String pid) {

        rideParticipants.add(currentUser);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        final Ride ride = new Ride();
        ride.setName(name);
        ride.setOwnerUID(currentUser.getUid());
        ride.setSource(src);
        ride.setDestination(dest);
        ride.setDate(date);
        ride.setTime(time);
        ride.setPid(pid);
        ride.setRid(dbRef.child("rides").push().getKey());

        Intent data = new Intent();
        data.putExtra("ride", ride);
        setResult(RESULT_OK, data);

        dbRef.child("rides").child(ride.getRid()).setValue(ride);

        //Upload contact pictures
        for (User participant : rideParticipants) {
            if (participant.getEmail() == null) {
                uploadImageDatabase(participant);
            }
        }
        List<UserInRide> rideUsers = new ArrayList<>();
        for (User participant : rideParticipants) {
            UserInRide user = new UserInRide();

            if (participant.getUid() == null) {
                String uid = UUID.randomUUID().toString();
                participant.setUid(uid);
                dbRef.child("users").child(uid).setValue(participant);
                user.setContactUser(true);
                //SEND VERIFICATION CODE TO PHONE USERS!!
                //sendVerificationCode(ride.getName(), ride.getRid(), index + "", participant.getPhone());
            }

            user.setUid(participant.getUid());
            if (participant.getUid().equals(currentUser.getUid())) {
                LatLng latLng = getAddressLatLng(src);
                if (latLng != null) {
                    user.setLatitude(latLng.latitude + "");
                    user.setLongitude(latLng.longitude + "");
                    user.setInRide(true);
                }
            } else
                user.setInRide(false);

            rideUsers.add(user);
        }

        dbRef.child("rideUsers").child(ride.getRid()).setValue(rideUsers);

        for (User participant : rideParticipants) {
            if (participant.getUid() != null && participant.getEmail() != null) {
                dbRef.child("userRides").child(participant.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> userRides = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                userRides.add(snap.getValue(String.class));
                            }
                        }
                        userRides.add(ride.getRid());
                        snapshot.getRef().setValue(userRides);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });
            }
        }

        rideParticipants.remove(currentUser);
        for (final User participant : rideParticipants) {
            if (participant.getUid() != null) {
                dbRef.child("tokens").child(participant.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String token = snapshot.getValue(String.class);
                            sendNotification(token, currentUser.displayName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });
            }
        }
        Toast.makeText(AddRideActivity.this, R.string.ride_added, Toast.LENGTH_SHORT).show();
        finish();
    }


    @Override
    public void onBackPressed() {
        if (saving)
            Toast.makeText(this, R.string.saving_changes, Toast.LENGTH_SHORT).show();
        else
            super.onBackPressed();
    }

    private class ViewPager2Adapter extends FragmentStateAdapter {

        public ViewPager2Adapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new DetailsFragment(AddRideActivity.this);
                case 1:
                    return new ParticipantsFragment(AddRideActivity.this);
                default:
                    return new Fragment();
            }
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);


        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    private void sendNotification(String tokenID, String name) {
        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("name", name);
            notification.put("to", tokenID);
            notification.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e(TAG, "onCreate: " + e.getMessage());
        }
        String URL = "https://fcm.googleapis.com/fcm/send";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, notification, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("JSON_OBJECT:", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("JSON_OBJECT_ERROR: ", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "key=AAAAuQUz1NI:APA91bHTVAC3T8FfWqsP_lX1Kd81L3HuKbkZgwp5BsCvk_gpm2guosbRyI-slC7-NRhyAHHYteAo2v-eszNLIYDshW9R_y6Lu1kSiDDSKiYCKSnQa7eISW2HBzSuMFQ-TUiyOyInxplO");
                header.put("Content-Type", "application/json");
                return header;
            }
        };
        RequestQueue mRequestQueue;
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mRequestQueue.add(request);
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
}