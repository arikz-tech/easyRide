package arikz.easyride.ui.main.profile;

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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import arikz.easyride.R;
import arikz.easyride.objects.User;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = ".EditProfileActivity";
    private static final int LOCATION_REQUEST_CODE = 19;

    private TextInputEditText etFirst, etLast, etPhone, etAddress;
    private ImageView ivProfile;
    private ProgressBar pbEdit, pbLoadingPic, pbAddress;

    private User loggedInUser;
    private String oldPID;
    private Uri filePath = null;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //Attach layout component
        pbLoadingPic = findViewById(R.id.pbLoadingPic);
        pbAddress = findViewById(R.id.pbAddress);
        etFirst = findViewById(R.id.etFirst);
        etLast = findViewById(R.id.etLast);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        ivProfile = findViewById(R.id.ivProfile);
        pbEdit = findViewById(R.id.pbEdit);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        FloatingActionButton fabPicEdit = findViewById(R.id.fabPicEdit);

        loggedInUser = Objects.requireNonNull(getIntent().getExtras()).getParcelable("user");
        assert loggedInUser != null;
        etFirst.setText(loggedInUser.getFirst());
        etLast.setText(loggedInUser.getLast());
        etPhone.setText(loggedInUser.getPhone());
        etAddress.setText(loggedInUser.getAddress());
        oldPID = loggedInUser.getPid();
        setProfilePicture();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saving = true;
                if (Objects.requireNonNull(etFirst.getText()).toString().isEmpty() || Objects.requireNonNull(etLast.getText()).toString().isEmpty()
                        || Objects.requireNonNull(etPhone.getText()).toString().isEmpty() || Objects.requireNonNull(etAddress.getText()).toString().isEmpty())
                    Toast.makeText(EditProfileActivity.this, getText(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                else {
                    pbEdit.setVisibility(View.VISIBLE);
                    String first = etFirst.getText().toString().trim();
                    String last = etLast.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();

                    if (placeExist(address)) {
                        Map<String, Object> editUser = new HashMap<>();
                        editUser.put("first", first);
                        editUser.put("last", last);
                        editUser.put("phone", phone);
                        editUser.put("address", address);

                        loggedInUser.setFirst(first);
                        loggedInUser.setLast(last);
                        loggedInUser.setPhone(phone);
                        loggedInUser.setAddress(address);

                        String uid = getCurrentUserId();
                        if (uid != null) {
                            FirebaseDatabase.getInstance().getReference().
                                    child("users").child(uid).updateChildren(editUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Intent data = new Intent();
                                    data.putExtra("user", loggedInUser);
                                    if (filePath != null) {
                                        loggedInUser.setPid(UUID.randomUUID().toString());
                                        uploadImage();
                                        setResult(RESULT_OK, data);
                                    } else {
                                        setResult(RESULT_OK, data);
                                        finish();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                                    Intent data = new Intent();
                                    data.putExtra("exception", e.getMessage());
                                    setResult(RESULT_CANCELED);
                                    finish();
                                }
                            });
                        }
                    } else
                        pbEdit.setVisibility(View.INVISIBLE);
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
        });
    }

    private boolean placeExist(String strAddress) {

        Geocoder coder = new Geocoder(getApplicationContext());
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);

            if (address == null) {
                Toast.makeText(getApplicationContext(), R.string.address_not_found, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (address.isEmpty()) {
                Toast.makeText(getApplicationContext(), R.string.address_not_found, Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                filePath = result.getUri();
                Glide.with(this).load(filePath).into(ivProfile);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.d(TAG, error + "");
            }
        }
    }

    private void uploadImage() {
        if (filePath != null) {
            FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(loggedInUser.getPid()).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if (oldPID != null) {
                        Objects.requireNonNull(taskSnapshot.getStorage().getParent()).child(oldPID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                savePictureID();
                            }
                        });
                    } else {
                        savePictureID();
                    }
                }
            });
        }
    }

    private void savePictureID() {
        final String uid = getCurrentUserId();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference().
                    child("users").child(uid).child("pid").setValue(loggedInUser.getPid()).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    finish();
                }
            });
        }
    }

    private void setProfilePicture() {
        pbLoadingPic.setVisibility(View.VISIBLE);
        if (oldPID != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(oldPID);

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

    private void takeUserCurrentPosition() {
        pbAddress.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            final LocationManager locationManager = (LocationManager) Objects.requireNonNull(getApplicationContext()).getSystemService(Context.LOCATION_SERVICE);
            class Listener implements LocationListener {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    List<Address> addresses;
                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(Objects.requireNonNull(location).getLatitude(), location.getLongitude(), 1);
                        etAddress.setText(addresses.get(0).getAddressLine(0));
                        Objects.requireNonNull(locationManager).removeUpdates(this);
                        pbAddress.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Listener listener = new Listener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5, listener);
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

    @Override
    public void onBackPressed() {
        if (saving)
            Toast.makeText(this, R.string.saving_changes, Toast.LENGTH_SHORT).show();
        else
            super.onBackPressed();
    }


}