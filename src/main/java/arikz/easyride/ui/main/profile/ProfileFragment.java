package arikz.easyride.ui.main.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {
    private static final String TAG = ".ProfileFragment";

    private View view;
    private MaterialTextView tvFirst, tvLast, tvMail, tvPhone, tvAddress;
    private ImageView ivProfile;
    private ExtendedFloatingActionButton fabEdit;
    private ProgressBar pbLoadingPic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        displayUserInfo();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        pbLoadingPic = view.findViewById(R.id.pbLoadingPic);
        pbLoadingPic.setVisibility(View.VISIBLE);
        tvFirst = view.findViewById(R.id.tvFirstFill);
        tvLast = view.findViewById(R.id.tvLastFill);
        tvMail = view.findViewById(R.id.tvMailFill);
        tvPhone = view.findViewById(R.id.tvPhoneFill);
        tvAddress = view.findViewById(R.id.tvAddressFill);
        fabEdit = view.findViewById(R.id.fabEdit);
        ivProfile = view.findViewById(R.id.ivProfilePic);
        ivProfile.setVisibility(View.INVISIBLE);

        fabEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });
    }

    public void displayUserInfo() {
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
        tvFirst.setText(user.getFirst());
        tvLast.setText(user.getLast());
        tvMail.setText(user.getEmail());
        tvPhone.setText(user.getPhone());
        tvAddress.setText(user.getAddress());

        String pid = user.getPid();
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);
            Context context = getContext();
            assert context != null;
            Glide.with(context).load(imageRef).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    pbLoadingPic.setVisibility(View.INVISIBLE);
                    ivProfile.setVisibility(View.VISIBLE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    pbLoadingPic.setVisibility(View.INVISIBLE);
                    ivProfile.setVisibility(View.VISIBLE);
                    return false;
                }
            }).into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.avatar_logo);
            pbLoadingPic.setVisibility(View.INVISIBLE);
            ivProfile.setVisibility(View.VISIBLE);
        }
    }

}