package arikz.easyride.ui.main.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import arikz.easyride.R;
import arikz.easyride.ui.main.profile.EditProfileActivity;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private static int EDIT_REQUEST_CODE = 4;

    private View view;
    private MaterialTextView tvFirst, tvLast, tvMail, tvPhone;
    private ImageView ivProfile;
    private ExtendedFloatingActionButton fabEdit;
    private String pictureID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        tvFirst = view.findViewById(R.id.tvFirstFill);
        tvLast = view.findViewById(R.id.tvLastFill);
        tvMail = view.findViewById(R.id.tvMailFill);
        tvPhone = view.findViewById(R.id.tvPhoneFill);
        fabEdit = view.findViewById(R.id.fabEdit);
        ivProfile = view.findViewById(R.id.ivProfile);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            tvFirst.setText(bundle.getCharSequence("first"));
            tvLast.setText(bundle.getCharSequence("last"));
            tvMail.setText(bundle.getCharSequence("email"));
            tvPhone.setText(bundle.getCharSequence("phone"));
            pictureID = bundle.getString("pid");
            setProfilePicture();
        }

        fabEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                intent.putExtra("first", tvFirst.getText());
                intent.putExtra("last", tvLast.getText());
                intent.putExtra("email", tvMail.getText());
                intent.putExtra("phone", tvPhone.getText());
                intent.putExtra("pid", pictureID);
                startActivityForResult(intent, EDIT_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_REQUEST_CODE) {
            if (data != null && data.getExtras() != null) {
                if (resultCode == RESULT_OK) {
                    tvFirst.setText(data.getExtras().getString("first"));
                    tvLast.setText(data.getExtras().getString("last"));
                    tvMail.setText(data.getExtras().getString("email"));
                    tvPhone.setText(data.getExtras().getString("phone"));
                    pictureID = data.getExtras().getString("pid");
                    setProfilePicture();
                    Toast.makeText(getContext(), R.string.edit_success, Toast.LENGTH_SHORT).show();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getContext(), data.getExtras().getString("exception"), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setProfilePicture() {
        if (pictureID != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pictureID);

            Glide.with(this).load(imageRef).into(ivProfile);
        }
    }
}