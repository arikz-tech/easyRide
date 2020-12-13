package arikz.easyride.ui.main.friends;

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
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.adapters.FriendsAdapter;
import arikz.easyride.models.User;
import arikz.easyride.util.LoadContacts;

public class FriendsFragment extends Fragment implements FriendsAdapter.OnFriendClicked {
    private static final String TAG = ".FriendsFragment";
    private static final int CONTACT_REQUEST_CODE = 15;
    private View view;
    private List<User> friends;
    private FriendsAdapter friendsAdapter;
    private User loggedInUser;
    private ExtendedFloatingActionButton fabInviteFriends;

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

        loggedInUser = Objects.requireNonNull(getArguments()).getParcelable("user");

        RecyclerView rvFriends = view.findViewById(R.id.rvFriends);
        rvFriends.setHasFixedSize(true);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        friends = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friends, this, getContext());
        rvFriends.setAdapter(friendsAdapter);

        collectContactFriends();

        fabInviteFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getText(R.string.share_intent_message));
                sendIntent.setType("text/plain");
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });
    }

    private void collectContactFriends() {
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_REQUEST_CODE);
        else
            fetchContact();

    }

    private void fetchContact() {
        LoadContacts loadContacts = new LoadContacts(getContext());
        final List<String> phonesList = loadContacts.getContactsPhoneNumbers();

        FirebaseDatabase.getInstance().getReference().
                child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    User friend = snap.getValue(User.class);
                    if (phonesList.contains(Objects.requireNonNull(friend).getPhone())) {
                        if (friend.getEmail() != null) {
                            if (!friends.contains(friend) && !friend.getPhone().equals(loggedInUser.getPhone()))
                                friends.add(friend);
                        }
                    }

                    friendsAdapter.notifyDataSetChanged();
                }

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
                Toast.makeText(getContext(), R.string.permission_importance, Toast.LENGTH_SHORT).show();
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

