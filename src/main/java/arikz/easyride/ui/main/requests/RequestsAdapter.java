package arikz.easyride.ui.main.requests;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.Ride;
import arikz.easyride.models.User;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {
    private final static String TAG = ".RequestsAdapter";
    private List<Ride> rides;
    private Activity activity;
    private OnRequestClicked requestFrag;
    public ViewHolder viewHolder;
    private int lastPosition = -1;

    public interface OnRequestClicked {
        void onClick(int index, MaterialButton button, ProgressBar progressBar);
    }


    public RequestsAdapter(Activity activity, Fragment requestFrag, List<Ride> rides) {
        this.rides = rides;
        this.requestFrag = (OnRequestClicked) requestFrag;
        this.activity = activity;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar;
        MaterialTextView tvRideOwner, tvRideName, tvSrc, tvDest, tvDate;
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
            tvDate = itemView.findViewById(R.id.tvDateFill);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = rides.indexOf((Ride) itemView.getTag());
                    requestFrag.onClick(index, btnConfirm, pbConfirm);
                }
            });
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
        holder.tvDate.setText(ride.getDate());
        getOwnerInfo(holder, ride.getOwnerUID());
        setAnimation(holder.itemView, position);
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

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

}
