package arikz.easyride.ui.main.rides;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;
import arikz.easyride.ui.main.rides.add.adapters.ParticipantsAdapter;
import arikz.easyride.ui.main.rides.map.MapActivity;

public class RideInfoActivity extends AppCompatActivity {
    private static final String TAG = ".RideInfoActivity";
    private ProgressBar pbRideInfo, pbMap;
    private FloatingActionButton fabMap;
    private ImageView ivRidePic;
    private Ride ride;
    private ParticipantsAdapter participantsAdapter;
    private List<UserInRide> participants;
    private Bundle imagesBundle;
    private long imgCnt, totalParticipants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_info);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.toolbarLayout);
        MaterialTextView tvSrcFill = findViewById(R.id.tvSrcFill);
        MaterialTextView tvDestFill = findViewById(R.id.tvDestFill);
        MaterialTextView tvDateFill = findViewById(R.id.tvDateFill);
        MaterialButton btnDelete = findViewById(R.id.btnDelete);

        ivRidePic = findViewById(R.id.ivRidePic);
        pbRideInfo = findViewById(R.id.pbRideInfo);
        pbMap = findViewById(R.id.pbMap);
        fabMap = findViewById(R.id.fabMap);
        imagesBundle = new Bundle();
        RecyclerView rvParticipants = findViewById(R.id.rvParticipants);
        participants = new ArrayList<>();

        participantsAdapter = new ParticipantsAdapter(participants, RideInfoActivity.this);
        rvParticipants.setAdapter(participantsAdapter);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(toolbar);

        ride = Objects.requireNonNull(getIntent().getExtras()).getParcelable("ride");
        toolbarLayout.setTitle(Objects.requireNonNull(ride).getName());
        setRideImage();

        tvSrcFill.setText(ride.getSource());
        tvDestFill.setText(ride.getDestination());
        tvDateFill.setText(ride.getDate());

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

        fabMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RideInfoActivity.this, MapActivity.class);
                intent.putParcelableArrayListExtra("users", (ArrayList<UserInRide>) participants);
                intent.putExtra("images", imagesBundle);
                startActivity(intent);
            }
        });
    }

    private void exitRide() {
        pbRideInfo.setVisibility(View.VISIBLE);
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

    private boolean isOwner() {
        return ride.getOwnerUID().equals(getCurrentUserId());
    }

    private void collectParticipants() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setTotalParticipants(snapshot.getChildrenCount());
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String key = snap.getKey();
                    UserInRide user = snap.getValue(UserInRide.class);
                    if (Objects.requireNonNull(user).isInRide()) {
                        participants.add(0, user); //Add participant into front of array list
                        addParticipantImage(user.getUid());
                    } else {
                        isImagesUploaded();
                        participants.add(user);
                    }


                    if (Integer.parseInt(Objects.requireNonNull(key)) == snapshot.getChildrenCount() - 1) {
                        try {
                            Thread.sleep((long) 0.1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        participantsAdapter.notifyDataSetChanged();
                        pbRideInfo.setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void addParticipantImage(String uid) {
        FirebaseDatabase.getInstance().getReference().
                child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final User user = snapshot.getValue(User.class);
                if (Objects.requireNonNull(user).getPid() != null) {
                    Task<byte[]> task = FirebaseStorage.getInstance().getReference().
                            child("images").child("users").child(user.getPid()).getBytes(Long.MAX_VALUE);
                    task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            imagesBundle.putByteArray(user.getPid(), bytes);
                            if (isImagesUploaded()) {
                                fabMap.setVisibility(View.VISIBLE);
                                pbMap.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                } else {
                    if (isImagesUploaded()) {
                        fabMap.setVisibility(View.VISIBLE);
                        pbMap.setVisibility(View.INVISIBLE);
                    }
                }
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

    private void deleteRideOwner() {
        pbRideInfo.setVisibility(View.VISIBLE);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
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

    public synchronized void setTotalParticipants(long total) {
        imgCnt = total;
    }

    public synchronized boolean isImagesUploaded() {
        return --imgCnt == 0;
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }
}