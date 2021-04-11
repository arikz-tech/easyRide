package arik.easyride.ui.main.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import arik.easyride.R;
import arik.easyride.models.User;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = ".EditProfileActivity";
    private static final int LOCATION_REQUEST_CODE = 19;

    private TextInputEditText etFirst, etLast, etPhone, etAddress;
    private ImageView ivProfile;
    private ProgressBar pbEdit, pbLoadingPic;
    private String pid;
    private Uri filePath = null;
    private boolean saving, changeImage, addressClicked;
    private LocationManager locationManager;
    private AutoCompleteTextView etArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //Attach layout component
        pbLoadingPic = findViewById(R.id.pbLoadingPic);
        pbEdit = findViewById(R.id.pbEdit);
        etFirst = findViewById(R.id.etFirst);
        etLast = findViewById(R.id.etLast);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        ivProfile = findViewById(R.id.ivProfile);
        etArea = findViewById(R.id.etArea);

        MaterialButton btnSave = findViewById(R.id.btnSave);
        FloatingActionButton fabPicEdit = findViewById(R.id.fabPicEdit);

        displayUserInfo();

        String[] areas = {"050", "051", "052", "053","054"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, areas);
        etArea.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!saving) {
                    saving = true;
                    boolean noEmptyFields = !etFirst.getText().toString().isEmpty() || !etLast.getText().toString().isEmpty();

                    if (noEmptyFields) {
                        pbEdit.setVisibility(View.VISIBLE);
                        String first = etFirst.getText().toString().trim();
                        String last = etLast.getText().toString().trim();
                        String phone = etArea.getText().toString().trim() + etPhone.getText().toString().trim();
                        String address = etAddress.getText().toString().trim();

                        if (isAddressValid(address) || address.isEmpty()) {
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            assert currentUser != null;
                            String uid = currentUser.getUid();
                            User user = new User();
                            user.setUid(uid);
                            user.setEmail(currentUser.getEmail());
                            user.setFirst(first);
                            user.setLast(last);
                            user.setPhone(phone);
                            user.setAddress(address);
                            user.setPid(pid);
                            FirebaseDatabase.getInstance().getReference().
                                    child("users").child(user.getUid()).setValue(user);

                            if (changeImage) {
                                uploadImage();
                            } else {
                                finish();
                            }
                        } else {
                            pbEdit.setVisibility(View.INVISIBLE);
                            Toast.makeText(EditProfileActivity.this, R.string.address_not_found, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.enter_fields, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        fabPicEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(EditProfileActivity.this);
            }
        });

        etAddress.setShowSoftInputOnFocus(false);
        etAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (!addressClicked) {
                        addressClicked = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
                        builder.setTitle(R.string.address);
                        builder.setMessage(R.string.take_current_pos);
                        builder.setIcon(R.drawable.ic_address_24);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                takeUserCurrentPosition();
                            }
                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                etAddress.setShowSoftInputOnFocus(true);
                            }
                        }).show();
                    }
                }
            }
        });
    }

    private void displayUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        FirebaseDatabase.getInstance().getReference().
                child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                assert currentUser != null;
                updateUI(currentUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void updateUI(User user) {
        pbLoadingPic.setVisibility(View.VISIBLE);
        etFirst.setText(user.getFirst());
        etLast.setText(user.getLast());
        etLast.setText(user.getLast());
        etAddress.setText(user.getAddress());
        etPhone.setText(user.getPhone());

        pid = user.getPid();
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
            }).into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.avatar_logo);
            pbLoadingPic.setVisibility(View.INVISIBLE);
        }

    }

    private boolean isAddressValid(String address) {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            return !addresses.isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                filePath = result.getUri();
                Glide.with(EditProfileActivity.this).load(filePath).into(ivProfile);
                pid = UUID.randomUUID().toString();
                changeImage = true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.d(TAG, error + "");
            }
        }
    }

    private void uploadImage() {
        if (filePath != null) {
            FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void takeUserCurrentPosition() {

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            trackPosition();
        }
    }

    private void trackPosition() {
        pbEdit.setVisibility(View.VISIBLE);
        class Listener implements LocationListener {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                List<Address> addresses;
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                try {
                    addresses = geocoder.getFromLocation(Objects.requireNonNull(location).getLatitude(), location.getLongitude(), 1);
                    etAddress.setText(addresses.get(0).getAddressLine(0));
                    locationManager.removeUpdates(this);
                    pbEdit.setVisibility(View.INVISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Listener listener = new Listener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                trackPosition();
            } else {
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (saving)
            Toast.makeText(this, R.string.saving_changes, Toast.LENGTH_SHORT).show();
        else
            super.onBackPressed();
    }


}