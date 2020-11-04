package arikz.easyride.ui.main.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import arikz.easyride.R;
import arikz.easyride.objects.User;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = ".EditProfileActivity";

    private TextInputEditText etFirst, etLast, etPhone;
    private ImageView ivProfile;
    private ProgressBar pbEdit, pbLoadingPic;

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
        etFirst = findViewById(R.id.etFirst);
        etLast = findViewById(R.id.etLast);
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
        oldPID = loggedInUser.getPid();
        setProfilePicture();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saving = true;
                if (Objects.requireNonNull(etFirst.getText()).toString().isEmpty() || Objects.requireNonNull(etLast.getText()).toString().isEmpty()
                        || Objects.requireNonNull(etPhone.getText()).toString().isEmpty())
                    Toast.makeText(EditProfileActivity.this, getText(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                else {
                    pbEdit.setVisibility(View.VISIBLE);
                    String firstName = etFirst.getText().toString().trim();
                    String lastName = etLast.getText().toString().trim();
                    String phoneNumber = etPhone.getText().toString().trim();

                    Map<String, Object> editUser = new HashMap<>();
                    editUser.put("first", firstName);
                    editUser.put("last", lastName);
                    editUser.put("phone", phoneNumber);

                    loggedInUser.setFirst(firstName);
                    loggedInUser.setLast(lastName);
                    loggedInUser.setPhone(phoneNumber);

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