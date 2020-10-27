package arikz.easyride.ui.main.rides.add.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.User;

public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.ViewHolder> {
    private final static String TAG = ".RidesAdapter";
    private List<Ride> rides;
    private OnRideClicked ridesFrag;
    private Context context;

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
        private MaterialTextView tvRideOwner, tvRideName, tvSrc, tvDest;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivRidePic = itemView.findViewById(R.id.ivRidePic);
            tvRideOwner = itemView.findViewById(R.id.tvRideOwner);
            tvRideName = itemView.findViewById(R.id.tvRideName);
            tvSrc = itemView.findViewById(R.id.tvSrcFill);
            tvDest = itemView.findViewById(R.id.tvDestFill);
            cvRide = itemView.findViewById(R.id.cvRide);
            cvOwner = itemView.findViewById(R.id.cvOwner);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cvRide.setBackground(getRippleEffect());
                    cvOwner.setBackground(getRippleEffect());
                    int index = rides.indexOf(itemView.getTag());
                    ridesFrag.onClick(index);
                }
            });
        }

        public RippleDrawable getRippleEffect() {
            int pressColor = ContextCompat.getColor(context, R.color.colorPrimary);
            ColorStateList csl = new ColorStateList(new int[][]{new int[]{}}, new int[]{pressColor});
            ColorDrawable cd = new ColorDrawable(Color.WHITE);
            return new RippleDrawable(csl, cd, null);
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
        setRideImage(holder.itemView, holder.ivRidePic, ride.getPid());

        getOwnerInfo(holder, ride.getOwnerUID());
    }

    private void getOwnerInfo(final ViewHolder holder, String uid) {
        FirebaseDatabase.getInstance().getReference().
                child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                holder.tvRideOwner.setText(user.displayName());
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
        }else ivRidePic.setImageResource(R.drawable.card_view_sample);
    }

}
