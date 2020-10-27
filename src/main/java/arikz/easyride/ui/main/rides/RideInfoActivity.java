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

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;
import arikz.easyride.ui.main.rides.add.adapters.ParticipantsAdapter;
import arikz.easyride.ui.main.rides.map.MapActivity;

public class RideInfoActivity extends AppCompatActivity {
    private static final String TAG = ".RideInfoActivity";
    private long totalThread, cntThreads;
    private ProgressBar pbRideInfo;
    private FloatingActionButton fabMap;
    private MaterialToolbar toolbar;
    private ImageView ivRidePic;
    private Ride ride;
    private CollapsingToolbarLayout toolbarLayout;
    private MaterialTextView tvSrcFill, tvDestFill;
    private RecyclerView rvParticipants;
    private MaterialButton btnDelete;
    private ParticipantsAdapter participantsAdapter;
    private List<UserInRide> participants;
    private Bundle imagesBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_info);
        toolbar = findViewById(R.id.toolbar);
        ivRidePic = findViewById(R.id.ivRidePic);
        toolbarLayout = findViewById(R.id.toolbarLayout);
        tvSrcFill = findViewById(R.id.tvSrcFill);
        tvDestFill = findViewById(R.id.tvDestFill);
        btnDelete = findViewById(R.id.btnDelete);
        pbRideInfo = findViewById(R.id.pbRideInfo);
        fabMap = findViewById(R.id.fabMap);
        imagesBundle = new Bundle();
        rvParticipants = findViewById(R.id.rvParticipants);
        participants = new ArrayList<>();

        participantsAdapter = new ParticipantsAdapter(participants, RideInfoActivity.this);
        rvParticipants.setAdapter(participantsAdapter);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(toolbar);

        ride = getIntent().getExtras().getParcelable("ride");
        toolbarLayout.setTitle(ride.getName());
        setRideImage();

        tvSrcFill.setText(ride.getSource());
        tvDestFill.setText(ride.getDestination());

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
                intent.putParcelableArrayListExtra("users",(ArrayList) participants);
                //TODO Should wait until all the picture has uploaded, counter or something like that..
                intent.putExtra("images",imagesBundle);
                startActivity(intent);
            }
        });

    }

    private void exitRide() {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        final String uid = getCurrentUserId();
        dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String key = snap.getKey();
                    String rid = snap.getValue(String.class);
                    if (rid.equals(ride.getRid()))
                        dbRef.child("userRides").child(uid).child(key).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
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
                    if (user.getUid().equals(uid))
                        dbRef.child("rideUsers").child(ride.getRid()).child(key).child("inRide").setValue(false);
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
        pbRideInfo.setVisibility(View.VISIBLE);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                countTotalThread(snapshot);
                for (DataSnapshot snap : snapshot.getChildren()) {
                    UserInRide user = snap.getValue(UserInRide.class);
                    if (user.isInRide()){
                        participants.add(0, user);
                        addParticipantImage(user.getUid());
                    }
                    else
                        participants.add(user);

                    countThread();
                }

                if (allThreadFinished()) {
                    pbRideInfo.setVisibility(View.INVISIBLE);
                    participantsAdapter.notifyDataSetChanged();
                    cntThreads = 0;
                    totalThread = 0;
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
                if(user.getPid()!=null){
                    Task<byte[]> task = FirebaseStorage.getInstance().getReference().
                            child("images").child("users").child(user.getPid()).getBytes(Long.MAX_VALUE);
                    task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            imagesBundle.putByteArray(user.getPid(),bytes);
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,error.getMessage());
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
                countTotalThread(snapshot);
                for (DataSnapshot snap : snapshot.getChildren()) {
                    final String uid = snap.getKey();
                    dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String key = snap.getKey();
                                String rid = snap.getValue(String.class);
                                if (rid.equals(ride.getRid()))
                                    dbRef.child("userRides").child(uid).child(key).removeValue();
                            }

                            countThread();
                            if (allThreadFinished()) {
                                Intent data = new Intent();
                                data.putExtra("ride", ride.getRid());
                                setResult(RESULT_OK, data);
                                finish();
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

        if(ride.getPid()!=null)
            FirebaseStorage.getInstance().getReference().
                child("images").child("rides").child(ride.getPid()).delete();

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


    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }
}