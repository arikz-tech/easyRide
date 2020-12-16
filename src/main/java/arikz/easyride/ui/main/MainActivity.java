package arikz.easyride.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;
import arikz.easyride.ui.login.LoginActivity;
import arikz.easyride.ui.main.friends.FriendsFragment;
import arikz.easyride.ui.main.map.MapFragment;
import arikz.easyride.ui.main.profile.ProfileFragment;
import arikz.easyride.ui.main.requests.RequestsFragment;
import arikz.easyride.ui.main.rides.RidesFragment;
import arikz.easyride.ui.main.setting.SettingFragment;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = ".MainActivity";

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private MaterialTextView tvName, tvMail;
    private ImageView ivProfilePic;
    private RidesFragment ridesFragment;
    private BottomNavigationView bottomNavigationView;
    private Bundle userBundle;
    private ProgressBar pbLoadingPic;
    private MaterialToolbar toolbar;

    @Override
    protected void onStart() {
        super.onStart();
        getUserAndDisplayInfo();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        navigationView = findViewById(R.id.nav_view);
        drawer = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.topAppBar);
        setRidesDefaultFragment();

        //Set the bottom navigation view
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                switch (item.getItemId()) {
                    case R.id.rides:
                        toolbar.setTitle(getApplicationContext().getString(R.string.rides));
                        selectedFragment = ridesFragment;
                        break;
                    case R.id.requests:
                        toolbar.setTitle(getApplicationContext().getString(R.string.rides_requests));
                        selectedFragment = new RequestsFragment();
                        break;
                    case R.id.map:
                        toolbar.setTitle(getApplicationContext().getString(R.string.map));
                        selectedFragment = new MapFragment();
                        break;
                }

                item.setCheckable(true);
                item.setChecked(true);

                for (int i = 0; i < navigationView.getMenu().size(); i++)
                    navigationView.getMenu().getItem(i).setChecked(false);

                if (selectedFragment != null) {
                    selectedFragment.setArguments(userBundle);
                    getSupportFragmentManager().
                            beginTransaction().
                            replace(R.id.fragment_container, selectedFragment).
                            commit();
                    return true;
                } else
                    return false;
            }

        });


        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            boolean itemCheckedSign = false;

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.friends:
                        if (userBundle != null) {
                            itemCheckedSign = true;
                            toolbar.setTitle(getApplicationContext().getString(R.string.friends));
                            FriendsFragment friendsFragment = new FriendsFragment();
                            friendsFragment.setArguments(userBundle);
                            getSupportFragmentManager().
                                    beginTransaction().
                                    replace(R.id.fragment_container, friendsFragment).commit();
                        }
                        break;
                    case R.id.profile:
                        if (userBundle != null) {
                            itemCheckedSign = true;
                            toolbar.setTitle(getApplicationContext().getString(R.string.profile));
                            ProfileFragment profileFragment = new ProfileFragment();
                            profileFragment.setArguments(userBundle);
                            getSupportFragmentManager().
                                    beginTransaction().
                                    replace(R.id.fragment_container, profileFragment).commit();
                        }
                        break;
                    case R.id.setting:
                        itemCheckedSign = true;
                        toolbar.setTitle(getApplicationContext().getString(R.string.setting));
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
                if (itemCheckedSign) {
                    item.setCheckable(true);
                    item.setChecked(true);
                    itemCheckedSign = false;
                }

                bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        //Attach header layout
        View navHeader = navigationView.getHeaderView(0);
        tvName = navHeader.findViewById(R.id.tvFirst);
        tvMail = navHeader.findViewById(R.id.tvMail);
        ivProfilePic = navHeader.findViewById(R.id.ivProfilePic);
        pbLoadingPic = navHeader.findViewById(R.id.pbLoadingPic);

    }


    private void setRidesDefaultFragment() {
        toolbar.setTitle(getApplicationContext().getString(R.string.rides));
        ridesFragment = new RidesFragment();
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.fragment_container, ridesFragment).
                commit();
        bottomNavigationView.getMenu().getItem(1).setChecked(true);
    }

    private void getUserAndDisplayInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference().
                    child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    User loggedInUser = snapshot.getValue(User.class);

                    userBundle = new Bundle();
                    userBundle.putParcelable("user", loggedInUser);
                    ridesFragment.setArguments(userBundle);

                    if (loggedInUser != null) {
                        tvName.setText(Objects.requireNonNull(loggedInUser).displayName());
                        tvMail.setText(loggedInUser.getEmail());
                        setProfilePicture(loggedInUser.getPid());
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
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);
    }

    private void setProfilePicture(String pid) {
        pbLoadingPic.setVisibility(View.VISIBLE);

        StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                child("images").child("users").child(pid);

        Glide.with(this).load(imageRef).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                pbLoadingPic.setVisibility(View.INVISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                pbLoadingPic.setVisibility(View.INVISIBLE);
                return false;
            }
        }).into(ivProfilePic);
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
                profileFragment.setArguments(userBundle);
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
                        child("users").child(Objects.requireNonNull(getCurrentUserId())).child("phone").setValue("-");
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
