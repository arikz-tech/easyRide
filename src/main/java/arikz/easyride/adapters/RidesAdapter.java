package arikz.easyride.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.ViewHolder> {
    private final static String TAG = ".RidesAdapter";
    private List<Ride> rides;
    private OnRideClicked ridesFrag;
    private Context context;
    private int lastPosition = -1;

    public interface OnRideClicked {
        void onClick(int index);
    }

    public RidesAdapter(Context context, Fragment ridesFrag, List<Ride> rides) {
        this.rides = rides;
        this.ridesFrag = (OnRideClicked) ridesFrag;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cvRide, cvOwner;
        private ImageView ivAvatar, ivRidePic;
        private MaterialTextView tvRideOwner, tvRideName, tvSrc, tvDest, tvDate;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivRidePic = itemView.findViewById(R.id.ivRidePic);
            tvRideOwner = itemView.findViewById(R.id.tvRideOwner);
            tvRideName = itemView.findViewById(R.id.tvRideName);
            tvSrc = itemView.findViewById(R.id.tvSrcFill);
            tvDest = itemView.findViewById(R.id.tvDestFill);
            tvDate = itemView.findViewById(R.id.tvDateFill);
            cvRide = itemView.findViewById(R.id.cvRide);
            cvOwner = itemView.findViewById(R.id.cvOwner);

            cvRide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = rides.indexOf((Ride) itemView.getTag());
                    ridesFrag.onClick(index);
                }
            });
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rides_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ride ride = rides.get(position);
        holder.itemView.setTag(ride);
        holder.tvRideName.setText(ride.getName());
        holder.tvSrc.setText(ride.getSource());
        holder.tvDest.setText(ride.getDestination());
        holder.tvDate.setText(ride.getDate());
        setRideImage(holder.itemView, holder.ivRidePic, ride.getPid());
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

    private void setRideImage(View itemView, ImageView ivRidePic, String pid) {
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(pid);

            Glide.with(itemView).load(imageRef).into(ivRidePic);
        } else ivRidePic.setImageResource(R.drawable.card_view_sample);
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

}
