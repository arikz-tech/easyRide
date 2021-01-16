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
import arikz.easyride.adapters.AddFriendAdapter;
import arikz.easyride.models.ContactPerson;
import arikz.easyride.models.User;
import arikz.easyride.util.LoadContacts;

public class AddParticipantActivity extends AppCompatActivity implements AddFriendAdapter.AddParticipantListener {
    private static String TAG = ".AddParticipantActivity";
    private static final int CONTACT_REQUEST_CODE = 11;

    private List<User> participants;
    private AddFriendAdapter addParticipantsAdapter;
    private ProgressBar pbParticipants;
    private MaterialToolbar toolbar;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_participant);

        pbParticipants = findViewById(R.id.pbParticipants);
        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getCurrentUser();

        RecyclerView rvParticipants = findViewById(R.id.rvParticipants);
        rvParticipants.setHasFixedSize(true);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        participants = new ArrayList<>();
        addParticipantsAdapter = new AddFriendAdapter(participants, this);

        rvParticipants.setAdapter(addParticipantsAdapter);

        collectContactFriends();
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

    private void collectContactFriends() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_REQUEST_CODE);
        else
            fetchContact();

    }

    private void fetchContact() {
        LoadContacts loadContacts = new LoadContacts(getApplicationContext());
        final ArrayList<ContactPerson> contactList = loadContacts.getContactList();
        FirebaseDatabase.getInstance().getReference().
                child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    User friend = snap.getValue(User.class);
                    if (friend != null) {
                        ContactPerson contactFriend = new ContactPerson(friend.displayName(), friend.getPhone(), null);
                        if (contactList.contains(contactFriend)) {
                            if (friend.getEmail() != null) {
                                if (!participants.contains(friend) && !friend.getPhone().equals(currentUser.getPhone()))
                                    participants.add(friend);
                            }
                        }
                    }
                }
                addParticipantsAdapter.notifyDataSetChanged();
                pbParticipants.setVisibility(View.INVISIBLE);
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
                Toast.makeText(this, R.string.friends_permission_importance, Toast.LENGTH_SHORT).show();
        }
    }

}