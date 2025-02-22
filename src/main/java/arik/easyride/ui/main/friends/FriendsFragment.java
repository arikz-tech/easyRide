package arik.easyride.ui.main.friends;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import arik.easyride.R;
import arik.easyride.adapters.FriendsAdapter;
import arik.easyride.models.ContactPerson;
import arik.easyride.models.User;
import arik.easyride.util.LoadContacts;

public class FriendsFragment extends Fragment implements FriendsAdapter.OnFriendClicked {
    private static final String TAG = ".FriendsFragment";
    private static final int CONTACT_REQUEST_CODE = 15;
    private View view;
    private List<User> friends;
    private FriendsAdapter friendsAdapter;
    private ExtendedFloatingActionButton fabInviteFriends;
    private ProgressBar pbFriend;
    private User currentUser;
    private ImageView ivNoFriends;
    private TextView tvNoFriends;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_friends, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fabInviteFriends = view.findViewById(R.id.fabInviteFriends);
        ivNoFriends = view.findViewById(R.id.ivNoFriends);
        tvNoFriends = view.findViewById(R.id.tvNoFriends);
        pbFriend = view.findViewById(R.id.pbFriend);

        RecyclerView rvFriends = view.findViewById(R.id.rvFriends);
        rvFriends.setHasFixedSize(true);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        friends = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friends, this, getContext());
        rvFriends.setAdapter(friendsAdapter);

        getCurrentUser();
        collectContactFriends();

        fabInviteFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));
                sendIntent.setType("text/plain");
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });
    }

    private void collectContactFriends() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_REQUEST_CODE);
        else
            fetchContact();

    }

    private void fetchContact() {
        LoadContacts loadContacts = new LoadContacts(getContext());
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
                                if (currentUser != null) {
                                    if (!friends.contains(friend) && !friend.getPhone().equals(currentUser.getPhone()))
                                        friends.add(friend);
                                }
                            }
                        }
                    }
                }
                if (friends.isEmpty()){
                    ivNoFriends.setVisibility(View.VISIBLE);
                    tvNoFriends.setVisibility(View.VISIBLE);
                }
                friendsAdapter.notifyDataSetChanged();
                pbFriend.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACT_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchContact();
            } else
                Toast.makeText(getContext(), R.string.friends_permission_importance, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(int index) {
        Intent intent = new Intent(getContext(), FriendsInfoActivity.class);
        intent.putExtra("userInfo", friends.get(index));
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

}

