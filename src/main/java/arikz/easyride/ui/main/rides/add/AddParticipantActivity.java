package arikz.easyride.ui.main.rides.add;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;
import arikz.easyride.util.LoadContacts;
import arikz.easyride.adapters.AddParticipantsAdapter;

public class AddParticipantActivity extends AppCompatActivity implements AddParticipantsAdapter.AddParticipantListener {
    private static String TAG = ".AddParticipantActivity";
    private static final int CONTACT_REQUEST_CODE = 11;

    private List<User> participants;
    private AddParticipantsAdapter addParticipantsAdapter;
    private ProgressBar pbParticipants;
    private User loggedInUser;
    private MaterialToolbar toolbar;

    //TODO ADD THE ABILITY TO ADD PARTICIPANTS VIA PHONE NUMBER

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_participant);

        pbParticipants = findViewById(R.id.pbParticipants);
        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        loggedInUser = Objects.requireNonNull(getIntent().getExtras()).getParcelable("user");

        RecyclerView rvParticipants = findViewById(R.id.rvParticipants);
        rvParticipants.setHasFixedSize(true);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        participants = new ArrayList<>();
        addParticipantsAdapter = new AddParticipantsAdapter(participants, this);

        rvParticipants.setAdapter(addParticipantsAdapter);

        collectContactFriends();
    }

    private void collectContactFriends() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_REQUEST_CODE);
        else
            fetchContact();

    }

    private void fetchContact() {
        pbParticipants.setVisibility(View.VISIBLE);
        LoadContacts loadContacts = new LoadContacts(getApplicationContext());
        final List<String> phonesList = loadContacts.getContactsPhoneNumbers();

        FirebaseDatabase.getInstance().getReference().
                child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    User friend = snap.getValue(User.class);
                    if (phonesList.contains(Objects.requireNonNull(friend).getPhone())) {
                        if (friend.getEmail() != null) {
                            if (!participants.contains(friend) && !friend.getPhone().equals(loggedInUser.getPhone()))
                                participants.add(friend);
                        }
                    }

                    addParticipantsAdapter.notifyDataSetChanged();
                    pbParticipants.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    @Override
    public void onClick(int index) {
        Intent data = new Intent();
        data.putExtra("user", participants.get(index));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACT_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchContact();
            } else
                Toast.makeText(this, R.string.permission_importance, Toast.LENGTH_SHORT).show();
        }
    }
}