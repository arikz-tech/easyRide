package arik.easyride.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
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
import com.google.android.material.navigation.NavigationView;
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
import com.google.type.LatLng;

import java.util.ArrayList;
import java.util.List;

import arik.easyride.R;
import arik.easyride.models.User;
import arik.easyride.ui.login.LoginActivity;
import arik.easyride.ui.main.friends.FriendsFragment;
import arik.easyride.ui.main.map.MapFragment;
import arik.easyride.ui.main.profile.ProfileFragment;
import arik.easyride.ui.main.requests.RequestsFragment;
import arik.easyride.ui.main.rides.RidesFragment;
import arik.easyride.ui.main.setting.SettingFragment;
import arik.easyride.util.KMeans;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = ".MainActivity";

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private MaterialTextView tvName, tvMail;
    private ImageView ivProfilePic;
    private RidesFragment ridesFragment;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar pbLoadingPic;
    private MaterialToolbar toolbar;

    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationBarUserInfo();
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

        /////////////////////////////////////Test//////////////////////////
        List<KMeans.Point> points = new ArrayList<>();
        points.add(new KMeans.Point(3,3));
        points.add(new KMeans.Point(4,5));
        points.add(new KMeans.Point(5,4));
        points.add(new KMeans.Point(8,9));
        points.add(new KMeans.Point(11,8));
        points.add(new KMeans.Point(12,7));
        points.add(new KMeans.Point(12,11));
        points.add(new KMeans.Point(3,7));

        KMeans kMeans = new KMeans(points,3);
        kMeans.startCluster();

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
                    getSupportFragmentManager().
                            beginTransaction().
                            replace(R.id.fragment_container, selectedFragment).
                            commit();
                    return true;
                } else
                    return false;
            }

        });
        bottomNavigationView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

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
                        itemCheckedSign = true;
                        toolbar.setTitle(getApplicationContext().getString(R.string.friends));
                        getSupportFragmentManager().
                                beginTransaction().
                                replace(R.id.fragment_container, new FriendsFragment()).commit();

                        break;
                    case R.id.profile:
                        itemCheckedSign = true;
                        toolbar.setTitle(getApplicationContext().getString(R.string.profile));
                        getSupportFragmentManager().
                                beginTransaction().
                                replace(R.id.fragment_container, new ProfileFragment()).commit();

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
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));
                        sendIntent.setType("text/plain");
                        Intent shareIntent = Intent.createChooser(sendIntent, null);
                        startActivity(shareIntent);
                        break;
                    case R.id.logout:
                        signOut();
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

    public void signOut() {
        GoogleSignInClient googleClient = GoogleSignIn.getClient(MainActivity.this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        googleClient.signOut();
        FirebaseAuth.getInstance().signOut();
        Intent signOutIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(signOutIntent);
        finish();
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

    public void updateNavigationBarUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        FirebaseDatabase.getInstance().getReference().
                child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    updateUI(currentUser);

                    if (currentUser.getPhone() == null) {
                        showSetPhoneDialog(currentUser.getUid());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void showSetPhoneDialog(final String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.add_phone_number);
        builder.setMessage(R.string.add_phone_number_message);
        View viewInflated = LayoutInflater.from(MainActivity.this).inflate(R.layout.phone_dialog_layout, (ViewGroup) findViewById(android.R.id.content).getRootView(), false);
        final View NameInput = viewInflated.findViewById(R.id.etNameLayout);
        final EditText PhoneInput = viewInflated.findViewById(R.id.etPhone);
        final AutoCompleteTextView etArea = viewInflated.findViewById(R.id.etArea);
        NameInput.setVisibility(View.GONE);

        String[] areas = {"050", "051", "052", "053", "054"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.select_dialog_item, areas);
        etArea.setAdapter(adapter);

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String phone = etArea.getText().toString().trim() + PhoneInput.getText().toString().trim();
                if (!phone.isEmpty()) {
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    dbRef.child("users").child(uid).child("phone").setValue(phone);
                } else
                    Toast.makeText(MainActivity.this, R.string.enter_fields, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void updateUI(User user) {
        pbLoadingPic.setVisibility(View.VISIBLE);
        tvName.setText(user.displayName());
        tvMail.setText(user.getEmail());

        String pid = user.getPid();
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);
            Glide.with(getApplicationContext()).load(imageRef).listener(new RequestListener<Drawable>() {
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
        } else {
            ivProfilePic.setImageResource(R.drawable.avatar_logo);
            pbLoadingPic.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

}
