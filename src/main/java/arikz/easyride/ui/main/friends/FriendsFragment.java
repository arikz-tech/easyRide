package arikz.easyride.ui.main.friends;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import arikz.easyride.R;
import arikz.easyride.data.User;

public class FriendsFragment extends Fragment {
    private static String TAG = ".FriendsFragment";
    private static final int CONTACT_REQUEST_CODE = 15;
    private View view;
    List<User> friends;
    FriendsAdapter friendsAdapter;
    ProgressBar pbFriends;
    String currentUserPhoneNumber;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_friends, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pbFriends = view.findViewById(R.id.pbFriends);

        Bundle bundle = this.getArguments();
        currentUserPhoneNumber = bundle.getString("phone");

        RecyclerView rvFriends = view.findViewById(R.id.rvFriends);
        rvFriends.setHasFixedSize(true);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        friends = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friends);
        rvFriends.setAdapter(friendsAdapter);

        collectContactFriends();
    }

    private void collectContactFriends() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_REQUEST_CODE);
        else
            fetchContact();

    }

    private void fetchContact() {
        pbFriends.setVisibility(View.VISIBLE);
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        ContentResolver resolver = getActivity().getContentResolver();
        Cursor cursor = resolver.query(uri, projection, null, null, null);

        final ArrayList<String> phoneNumbers = new ArrayList<>();

        while (cursor.moveToNext()) {
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            phoneNumbers.add(PhoneNumberUtils.normalizeNumber(phoneNumber));
        }

        FirebaseDatabase.getInstance().getReference().
                child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    User friend = snap.getValue(User.class);
                    if (phoneNumbers.contains(friend.getPhone())) {
                        if (!friends.contains(friend) && !friend.getPhone().equals(currentUserPhoneNumber))
                            friends.add(friend);
                    }

                    friendsAdapter.notifyDataSetChanged();
                    pbFriends.setVisibility(View.GONE);
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
                Toast.makeText(getContext(), "The permission is really important to see who in your friend list is already registered", Toast.LENGTH_SHORT).show();
        }


    }

}
