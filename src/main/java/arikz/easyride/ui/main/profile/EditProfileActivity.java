package arikz.easyride.ui.main.profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import java.util.UUID;

import arikz.easyride.R;

//TODO ADD DESCRIPTION !!

public class EditProfileActivity extends AppCompatActivity {

    private final static String TAG = ".EditProfileActivity";

    private TextInputEditText etFirst, etLast, etPhone;
    private ImageView ivProfile;
    private ProgressBar pbEdit;

    private String firstName, lastName, phoneNumber, mailAddress, oldPID, newPID;
    private Uri filePath = null;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //Attach layout component
        etFirst = findViewById(R.id.etFirst);
        etLast = findViewById(R.id.etLast);
        etPhone = findViewById(R.id.etPhone);
        ivProfile = findViewById(R.id.ivProfile);
        pbEdit = findViewById(R.id.pbEdit);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        FloatingActionButton fabPicEdit = findViewById(R.id.fabPicEdit);

        Bundle bundle = getIntent().getExtras();

        firstName = bundle.getString("first");
        lastName = bundle.getString("last");
        phoneNumber = bundle.getString("phone");
        mailAddress = bundle.getString("email");
        oldPID = bundle.getString("pid");

        etFirst.setText(firstName);
        etLast.setText(lastName);
        etPhone.setText(phoneNumber);
        setProfilePicture();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etFirst.getText().toString().isEmpty() || etLast.getText().toString().isEmpty()
                        || etPhone.getText().toString().isEmpty())
                    Toast.makeText(EditProfileActivity.this, getText(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                else {
                    pbEdit.setVisibility(View.VISIBLE);
                    firstName = etFirst.getText().toString().trim();
                    lastName = etLast.getText().toString().trim();
                    phoneNumber = etPhone.getText().toString().trim();

                    Map<String, Object> editUser = new HashMap<>();
                    editUser.put("first", firstName);
                    editUser.put("last", lastName);
                    editUser.put("phone", phoneNumber);

                    String uid = getCurrentUserId();
                    if (uid != null) {
                        FirebaseDatabase.getInstance().getReference().
                                child("users").child(uid).updateChildren(editUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Intent data = new Intent();
                                data.putExtra("first", firstName);
                                data.putExtra("last", lastName);
                                data.putExtra("email", mailAddress);
                                data.putExtra("phone", phoneNumber);
                                if (filePath != null) {
                                    newPID = UUID.randomUUID().toString();
                                    data.putExtra("pid", newPID);
                                    setResult(RESULT_OK, data);
                                    uploadImage();
                                } else {
                                    setResult(RESULT_OK, data);
                                    finish();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, e.getMessage());
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
            }
        }
    }

    private void uploadImage() {
        if (filePath != null) {
            saving = true;
            FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(newPID).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if (oldPID != null) {
                        taskSnapshot.getStorage().getParent().child(oldPID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
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
                    child("users").child(uid).child("pid").setValue(newPID).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    finish();
                }
            });
        }
    }

    private void setProfilePicture() {
        if (oldPID != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(oldPID);

            Glide.with(this).load(imageRef).into(ivProfile);
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
            Toast.makeText(this, "Saving changes, wait... ", Toast.LENGTH_SHORT).show();
        else
            super.onBackPressed();
    }
}