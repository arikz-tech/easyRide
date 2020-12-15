package arikz.easyride.ui.main.rides.add;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.telephony.SmsManager;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private static final int LOCATION_REQUEST_CODE = 14;

    private ViewPager viewPager;
    private List<User> rideParticipants;
    private User owner;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ride);

        rideParticipants = new ArrayList<>();
        owner = Objects.requireNonNull(getIntent().getExtras()).getParcelable("user");
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), 0);
        viewPagerAdapter.setFirstTabTitle(getText(R.string.ride_details));
        viewPagerAdapter.setSecondTabTitle(getText(R.string.participants));
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        Objects.requireNonNull(tabLayout.getTabAt(0)).setIcon(R.drawable.ic_rides_24);
        Objects.requireNonNull(tabLayout.getTabAt(1)).setIcon(R.drawable.ic_friends_24);
    }

    @Override
    public void onAdd(User participant) {
        rideParticipants.add(participant);
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
    public void onSubmit(String name, String src, String dest, String date, String pid) {

        rideParticipants.add(owner);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        final Ride ride = new Ride();
        ride.setName(name);
        ride.setOwnerUID(owner.getUid());
        ride.setSource(src);
        ride.setDestination(dest);
        ride.setDate(date);
        ride.setPid(pid);
        ride.setRid(dbRef.child("rides").push().getKey());

        Intent data = new Intent();
        data.putExtra("ride", ride);
        setResult(RESULT_OK, data);

        dbRef.child("rides").child(ride.getRid()).setValue(ride);

        List<UserInRide> rideUsers = new ArrayList<>();
        int index = 0;
        for (User participant : rideParticipants) {
            if (participant.getUid() == null) {
                String uid = UUID.randomUUID().toString();
                participant.setUid(uid);
                dbRef.child("users").child(uid).setValue(participant);
                //SEND VERIFICATION CODE TO PHONE USERS!!
                sendVerificationCode(ride.getRid(), index + "", participant.getPhone());
            }
            UserInRide user = new UserInRide();
            user.setUid(participant.getUid());
            if (participant.getUid().equals(owner.getUid())) {
                LatLng latLng = getAddressLatLng(src);
                if (latLng != null) {
                    user.setLatitude(latLng.latitude + "");
                    user.setLongitude(latLng.longitude + "");
                    user.setInRide(true);
                }
            } else
                user.setInRide(false);

            rideUsers.add(user);
            index++;
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

        rideParticipants.remove(owner);
        for (final User participant : rideParticipants) {
            if (participant.getUid() != null) {
                dbRef.child("tokens").child(participant.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String token = snapshot.getValue(String.class);
                            sendNotification(token, owner.displayName());
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

    private void sendVerificationCode(String rid, String index, String phone) {
        String message = getString(R.string.ride_invite);
        String url = "https://arikz-tech.github.io/easyrideconfirm?"
                + "rid=" + rid
                + "&index=" + index;
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phone, null, url, null, null);
    }

    @Override
    public void onBackPressed() {
        if (saving)
            Toast.makeText(this, R.string.saving_changes, Toast.LENGTH_SHORT).show();
        else
            super.onBackPressed();
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        CharSequence firstTabTitle, secondTabTitle;

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new DetailsFragment(AddRideActivity.this);
                case 1:
                    return new ParticipantsFragment(AddRideActivity.this);
                default:
                    //?
                    return new Fragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return firstTabTitle;
                case 1:
                    return secondTabTitle;
                default:
                    return null;
            }
        }

        public void setFirstTabTitle(CharSequence firstTabTitle) {
            this.firstTabTitle = firstTabTitle;
        }

        public void setSecondTabTitle(CharSequence secondTabTitle) {
            this.secondTabTitle = secondTabTitle;
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
                Log.d("JSON_OBJECT_ERROR: ", Objects.requireNonNull(error.getMessage()));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "key=AAAAuQUz1NI:APA91bG-whHcsvKtNMoqdeCD6jd3RuVDlDbajObAWzPr7AC6ULFvRL9MvVh5iOUjOdELcZtDzxwbaPGIdz1kD2mZgLA2Gdea_qIHIgvTtqj9UOw6RGkgStOBd67VW34UWVcpBMDwRt-b");
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