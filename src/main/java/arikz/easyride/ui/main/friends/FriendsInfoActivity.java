package arikz.easyride.ui.main.friends;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;
import arikz.easyride.ui.main.LoadContacts;
import arikz.easyride.ui.main.rides.map.ClusterMarker;
import arikz.easyride.ui.main.rides.map.MyClusterManagerRenderer;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;

public class FriendsInfoActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = ".FriendsInfoActivity";

    private MapView mapView;
    private GoogleMap mGoogleMap;
    private LatLngBounds mapBounds;
    private User user;
    private MaterialTextView tvFirst, tvLast, tvMail, tvPhone, tvAddress;
    private FloatingActionButton fabCall, fabMessage, fabWhatsApp;
    private ImageView ivProfile;
    private ProgressBar pbLoadingPic;
    private CardView cardView;
    private ClusterManager<ClusterMarker> clusterManager;
    private MyClusterManagerRenderer clusterManagerRenderer;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_info);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        user = Objects.requireNonNull(getIntent().getExtras()).getParcelable("userInfo");
        pbLoadingPic = findViewById(R.id.pbLoadingPic);
        tvFirst = findViewById(R.id.tvFirstFill);
        tvLast = findViewById(R.id.tvLastFill);
        tvMail = findViewById(R.id.tvMailFill);
        tvPhone = findViewById(R.id.tvPhoneFill);
        tvAddress = findViewById(R.id.tvAddressFill);
        ivProfile = findViewById(R.id.ivProfilePic);
        fabCall = findViewById(R.id.fabCall);
        fabMessage = findViewById(R.id.fabMessage);
        fabWhatsApp = findViewById(R.id.fabWhatsApp);

        mapView = findViewById(R.id.mvAddress);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        if (user != null) {
            tvFirst.setText(user.getFirst());
            tvLast.setText(user.getLast());
            tvMail.setText(user.getEmail());
            tvPhone.setText(user.getPhone());
            tvAddress.setText(user.getAddress());
            setProfilePicture(user.getPid());
        }

        fabCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user.getPhone() != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + user.getPhone()));
                    startActivity(intent);
                }
            }
        });

        fabMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:" + user.getPhone()));
                startActivity(intent);
            }
        });

        fabWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(Objects.requireNonNull(getApplicationContext()));
                String contactPhone = LoadContacts.formattedPhoneNumber(user.getPhone(), phoneUtil);
                //TODO Change to international
                String url = "https://api.whatsapp.com/send?phone=+972" + contactPhone;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

    }

    private void setProfilePicture(String pid) {
        pbLoadingPic.setVisibility(View.VISIBLE);
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(user.getPid());

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
            }).into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.avatar_logo);
            pbLoadingPic.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        List<Address> addresses;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocationName(Objects.requireNonNull(user).getAddress(), 1);
            LatLng latLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
            googleMap.setMinZoomPreference(12);
            addCluster(latLng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addCluster(final LatLng clusterLatLng) {
        if (clusterManager == null) {
            clusterManager = new ClusterManager<>(getApplicationContext(), mGoogleMap);
        }
        if (clusterManagerRenderer == null) {
            clusterManagerRenderer = new MyClusterManagerRenderer(getApplicationContext(), mGoogleMap, clusterManager);
            clusterManager.setRenderer(clusterManagerRenderer);
        }

        Task<byte[]> task = FirebaseStorage.getInstance().getReference().
                child("images").child("users").child(user.getPid()).getBytes(Long.MAX_VALUE);
        task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(final byte[] bytes) {
                FirebaseDatabase.getInstance().getReference().
                        child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User userInfo = snapshot.getValue(User.class);
                        String markerName = Objects.requireNonNull(userInfo).displayName();
                        ClusterMarker check = new ClusterMarker(clusterLatLng, markerName, user.getAddress(), bytes);
                        clusterManager.addItem(check);
                        clusterManager.cluster();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }

                });
            }
        });
    }
}