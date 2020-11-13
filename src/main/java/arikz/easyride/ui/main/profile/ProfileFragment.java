package arikz.easyride.ui.main.profile;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.User;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {
    private static final String TAG = ".ProfileFragment";
    private static int EDIT_REQUEST_CODE = 4;
    private View view;
    private MaterialTextView tvFirst, tvLast, tvMail, tvPhone,tvAddress;
    private ImageView ivProfile;
    private ExtendedFloatingActionButton fabEdit;
    private User loggedInUser;
    private ProgressBar pbLoadingPic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        pbLoadingPic = view.findViewById(R.id.pbLoadingPic);
        tvFirst = view.findViewById(R.id.tvFirstFill);
        tvLast = view.findViewById(R.id.tvLastFill);
        tvMail = view.findViewById(R.id.tvMailFill);
        tvPhone = view.findViewById(R.id.tvPhoneFill);
        tvAddress = view.findViewById(R.id.tvAddressFill);
        fabEdit = view.findViewById(R.id.fabEdit);
        ivProfile = view.findViewById(R.id.ivProfile);


        loggedInUser = Objects.requireNonNull(getArguments()).getParcelable("user");
        if (loggedInUser != null) {
            tvFirst.setText(loggedInUser.getFirst());
            tvLast.setText(loggedInUser.getLast());
            tvMail.setText(loggedInUser.getEmail());
            tvPhone.setText(loggedInUser.getPhone());
            tvAddress.setText(loggedInUser.getAddress());
            setProfilePicture(loggedInUser.getPid());
        }

        fabEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("user", loggedInUser);
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
                    loggedInUser = data.getExtras().getParcelable("user");
                    tvFirst.setText(Objects.requireNonNull(loggedInUser).getFirst());
                    tvLast.setText(loggedInUser.getLast());
                    tvMail.setText(loggedInUser.getEmail());
                    tvPhone.setText(loggedInUser.getPhone());
                    tvAddress.setText(loggedInUser.getAddress());
                    setProfilePicture(loggedInUser.getPid());
                    Toast.makeText(getContext(), R.string.edit_success, Toast.LENGTH_SHORT).show();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getContext(), data.getExtras().getString("exception"), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setProfilePicture(String pid) {
        pbLoadingPic.setVisibility(View.VISIBLE);
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(loggedInUser.getPid());

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
}