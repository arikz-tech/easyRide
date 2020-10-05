package arikz.easyride.ui.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import arikz.easyride.R;
import arikz.easyride.data.User;
import arikz.easyride.login.LoginActivity;
import arikz.easyride.ui.main.friends.FriendsFragment;
import arikz.easyride.ui.main.profile.ProfileFragment;
import arikz.easyride.ui.main.rides.RidesFragment;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = ".MainActivity";

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private View navHeader;
    private MaterialTextView tvName, tvMail;
    private ImageView ivProfilePic;
    private User loggedInUser;

    @Override
    protected void onStart() {
        super.onStart();
        getUserAndDisplayInfo();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set the default fragment when user open app to be rides fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new RidesFragment());

        //Set the bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                switch (item.getItemId()) {
                    case R.id.rides:
                        selectedFragment = new RidesFragment();
                        break;
                    case R.id.requests:
                        selectedFragment = new RequestsFragment();
                        break;
                    case R.id.map:
                        selectedFragment = new MapFragment();
                        break;
                }
                getSupportFragmentManager().
                        beginTransaction().
                        replace(R.id.fragment_container, selectedFragment).
                        commit();
                return true;
            }

        });

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Bundle bundle = new Bundle();
                switch (item.getItemId()) {
                    case R.id.friends:
                        FriendsFragment friendsFragment = new FriendsFragment();
                        bundle.putString("phone", loggedInUser.getPhone());
                        friendsFragment.setArguments(bundle);
                        getSupportFragmentManager().
                                beginTransaction().
                                replace(R.id.fragment_container, friendsFragment).commit();
                        break;
                    case R.id.profile:
                        ProfileFragment profileFragment = new ProfileFragment();
                        bundle.putString("first", loggedInUser.getFirst());
                        bundle.putString("last", loggedInUser.getLast());
                        bundle.putString("email", loggedInUser.getEmail());
                        bundle.putString("phone", loggedInUser.getPhone());
                        bundle.putString("pid", loggedInUser.getPid());
                        profileFragment.setArguments(bundle);
                        getSupportFragmentManager().
                                beginTransaction().
                                replace(R.id.fragment_container, profileFragment).commit();
                        break;
                    case R.id.setting:
                        getSupportFragmentManager().
                                beginTransaction().
                                replace(R.id.fragment_container, new SettingFragment()).commit();
                        break;
                    case R.id.share:
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getText(R.string.share_intent_message));
                        sendIntent.setType("text/plain");
                        Intent shareIntent = Intent.createChooser(sendIntent, null);
                        startActivity(shareIntent);
                        break;
                    case R.id.logout:
                        GoogleSignInClient googleClient = GoogleSignIn.getClient(MainActivity.this, GoogleSignInOptions.DEFAULT_SIGN_IN);
                        googleClient.signOut();
                        FirebaseAuth.getInstance().signOut();
                        Intent signOutIntent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(signOutIntent);
                        finish();
                        break;
                    case R.id.exit:
                        finish();
                        break;
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        //Attach header layout
        navHeader = navigationView.getHeaderView(0);
        tvName = navHeader.findViewById(R.id.tvFirst);
        tvMail = navHeader.findViewById(R.id.tvMail);
        ivProfilePic = navHeader.findViewById(R.id.ivProfilePic);

    }

    private void getUserAndDisplayInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference().
                    child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    loggedInUser = snapshot.getValue(User.class);
                    tvName.setText(loggedInUser.displayName());
                    tvMail.setText(loggedInUser.getEmail());

                    setProfilePicture();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    private void setProfilePicture() {
        String pid = loggedInUser.getPid();
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);

            Glide.with(this).load(imageRef).into(ivProfilePic);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    private void showPhoneAlertDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        CharSequence title = getText(R.string.alert_dialog_title_phone);
        CharSequence body = getText(R.string.alert_dialog_body_phone);

        builder.setTitle(title).setMessage(body).setIcon(R.drawable.ic_phone_24).setPositiveButton(R.string.alert_dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileFragment profileFragment = new ProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putCharSequence("first", loggedInUser.getFirst());
                bundle.putCharSequence("last", loggedInUser.getLast());
                bundle.putCharSequence("email", loggedInUser.getEmail());
                bundle.putCharSequence("phone", loggedInUser.getPhone());
                profileFragment.setArguments(bundle);
                getSupportFragmentManager().
                        beginTransaction().
                        replace(R.id.fragment_container, profileFragment).
                        commit();
                navigationView.setCheckedItem(R.id.profile);
                //TODO MARK Phone Edit on profile
            }
        }).setNegativeButton(R.string.alert_dialog_no_thanks, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseDatabase.getInstance().getReference().
                        child("users").child(getCurrentUserId()).child("phone").setValue("-");
                dialog.dismiss();
            }
        }).show();
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

}
