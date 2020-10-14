package arikz.easyride.ui.main.rides.add.tabs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.UUID;

import arikz.easyride.R;
import arikz.easyride.ui.main.rides.add.interfaces.DetailsEvents;

import static android.app.Activity.RESULT_OK;

public class DetailsFragment extends Fragment {
    private static final String TAG = "DetailsFragment";
    private View view;
    private TextInputEditText etName, etSrc, etDest;
    private ImageView ivRidePic;
    private MaterialButton btnAddRide,btnAddParticipants;
    private DetailsEvents event;
    private FloatingActionButton fabPicEdit;
    private RelativeLayout ivRidePicLayout;
    private ProgressBar pbAddRide;
    private Uri filePath = null;
    private TextInputLayout etNameLayout, etSrcLayout, etDestLayout;

    public DetailsFragment(Context context) {
        event = (DetailsEvents) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_details, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        etNameLayout = view.findViewById(R.id.etNameLayout);
        etSrcLayout = view.findViewById(R.id.etSrcLayout);
        etDestLayout = view.findViewById(R.id.etDestLayout);
        ivRidePic = view.findViewById(R.id.ivRidePic);
        etName = view.findViewById(R.id.etName);
        etSrc = view.findViewById(R.id.etSrc);
        etDest = view.findViewById(R.id.etDest);
        btnAddRide = view.findViewById(R.id.btnAddRide);
        fabPicEdit = view.findViewById(R.id.fabPicEdit);
        ivRidePicLayout = view.findViewById(R.id.ivRidePicLayout);
        pbAddRide = view.findViewById(R.id.pbAddRide);
        btnAddParticipants = view.findViewById(R.id.btnAddParticipants);

        btnAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(etName.getText().toString().isEmpty() || etSrc.getText().toString().isEmpty() || etDest.getText().toString().isEmpty())
                    Toast.makeText(getContext(), getString(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                else{
                    String rideName = etName.getText().toString();
                    String source = etSrc.getText().toString();
                    String destination = etDest.getText().toString();
                    uploadImageAndSubmit(rideName, source, destination);
                }
            }
        });

        fabPicEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(getContext(), DetailsFragment.this);
            }
        });

        btnAddParticipants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    event.onClickAddParticipants();
            }
        });
    }

    private void uploadImageAndSubmit(final String rideName, final String source, final String destination) {
        event.onImageUpload();
        pbAddRide.setVisibility(View.VISIBLE);
        ivRidePicLayout.setVisibility(View.INVISIBLE);
        etNameLayout.setVisibility(View.INVISIBLE);
        etSrcLayout.setVisibility(View.INVISIBLE);
        etDestLayout.setVisibility(View.INVISIBLE);
        btnAddRide.setVisibility(View.INVISIBLE);
        btnAddParticipants.setVisibility(View.INVISIBLE);

        if (filePath != null) {
            final String pid = UUID.randomUUID().toString();
            FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(pid).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    event.onSubmit(rideName, source, destination, pid);
                }
            });
        } else
            event.onSubmit(rideName, source, destination, null);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                filePath = result.getUri();

                Glide.with(this).load(filePath).into(ivRidePic);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(getActivity(), result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                Exception error = result.getError();
            }
        }
    }


}