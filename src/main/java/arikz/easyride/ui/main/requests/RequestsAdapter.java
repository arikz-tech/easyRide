package arikz.easyride.ui.main.requests;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {
    private final static String TAG = ".RequestsAdapter";
    private List<Ride> rides;
    private Activity activity;
    private OnRequestClicked requestFrag;
    public ViewHolder viewHolder;
    private boolean confirmed;

    public interface OnRequestClicked {
        void onClick(int index);
    }


    public RequestsAdapter(Activity activity, Fragment requestFrag, List<Ride> rides) {
        this.rides = rides;
        this.requestFrag = (OnRequestClicked) requestFrag;
        this.activity = activity;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar;
        MaterialTextView tvRideOwner, tvRideName, tvSrc, tvDest;
        MaterialButton btnConfirm;
        ProgressBar pbConfirm;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            pbConfirm = itemView.findViewById(R.id.pbConfirm);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvRideOwner = itemView.findViewById(R.id.tvRideOwner);
            tvRideName = itemView.findViewById(R.id.tvRideName);
            tvSrc = itemView.findViewById(R.id.tvSrcFill);
            tvDest = itemView.findViewById(R.id.tvDestFill);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!confirmed) {
                        int index = rides.indexOf((Ride) itemView.getTag());
                        requestFrag.onClick(index);
                        pbConfirm.setVisibility(View.VISIBLE);
                        btnConfirm.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        public void changeState(boolean confirm) {
            btnConfirm.setVisibility(View.VISIBLE);
            confirmed = confirm;
            if (confirm) {
                btnConfirm.setStrokeColorResource(R.color.colorPrimary);
                btnConfirm.setTextColor(activity.getColor(R.color.colorPrimary));
                btnConfirm.setText(R.string.confirmed);
            } else {
                btnConfirm.setStrokeColorResource(R.color.colorBlack);
                btnConfirm.setTextColor(activity.getColor(R.color.colorBlack));
                btnConfirm.setText(R.string.confirm);
            }
            pbConfirm.setVisibility(View.INVISIBLE);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.requests_row_layout, parent, false);
        viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ride ride = rides.get(position);
        holder.itemView.setTag(ride);
        holder.tvRideName.setText(ride.getName());
        holder.tvSrc.setText(ride.getSource());
        holder.tvDest.setText(ride.getDestination());

        getOwnerInfo(holder, ride.getOwnerUID());
    }

    private void getOwnerInfo(final ViewHolder holder, String uid) {
        FirebaseDatabase.getInstance().getReference().
                child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                holder.tvRideOwner.setText(Objects.requireNonNull(user).displayName());
                setOwnerImage(holder.itemView, holder.ivAvatar, user.getPid());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void setOwnerImage(View itemView, ImageView ivAvatar, String pid) {
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);

            Glide.with(itemView).load(imageRef).into(ivAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

}
