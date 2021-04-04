package arik.easyride.ui.main.friends;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.MapView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import arik.easyride.R;
import arik.easyride.models.User;
import arik.easyride.util.LoadContacts;

public class FriendsInfoActivity extends AppCompatActivity {
    private static final String TAG = ".FriendsInfoActivity";

    private MapView mapView;
    private User currentUser;
    private MaterialTextView tvFirst, tvLast, tvMail, tvPhone, tvAddress;
    private FloatingActionButton fabCall, fabMessage, fabWhatsApp;
    private ImageView ivProfile;
    private ProgressBar pbLoadingPic;


    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_info);


        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }


        if (getIntent().getExtras() != null) {
            currentUser = getIntent().getExtras().getParcelable("userInfo");
        }
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
        MapLoaded mapReady = new MapLoaded(this, currentUser);
        mapView.getMapAsync(mapReady);


        if (currentUser != null) {
            tvFirst.setText(currentUser.getFirst());
            tvLast.setText(currentUser.getLast());
            tvMail.setText(currentUser.getEmail());
            tvPhone.setText(currentUser.getPhone());
            tvAddress.setText(currentUser.getAddress());
            setProfilePicture();
        }

        fabCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentUser.getPhone()));
                startActivity(intent);
            }
        });

        fabMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:" + currentUser.getPhone()));
                startActivity(intent);

            }
        });

        fabWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contactPhone = LoadContacts.formattedPhoneNumber(currentUser.getPhone());
                String url = "https://api.whatsapp.com/send?phone=+972" + contactPhone;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

            }
        });

    }

    private void setProfilePicture() {
        pbLoadingPic.setVisibility(View.VISIBLE);

        String pid = currentUser.getPid();
        if (pid != null) {
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
            }).into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.avatar_logo);
            pbLoadingPic.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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

}