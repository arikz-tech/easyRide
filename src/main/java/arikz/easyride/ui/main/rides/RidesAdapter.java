package arikz.easyride.ui.main.rides;

import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import arikz.easyride.R;
import arikz.easyride.data.Ride;

public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.ViewHolder> {
    List<Ride> rides;

    public RidesAdapter(List<Ride> rides) {
        this.rides = rides;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivRidePic;
        MaterialTextView tvRideOwner, tvRideName, tvSrc, tvDest;
        MaterialButton btnParticipants;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivRidePic = itemView.findViewById(R.id.ivRidePic);
            tvRideOwner = itemView.findViewById(R.id.tvRideOwner);
            tvRideName = itemView.findViewById(R.id.tvRideName);
            tvSrc = itemView.findViewById(R.id.tvSrcFill);
            tvDest = itemView.findViewById(R.id.tvDestFill);
            btnParticipants = itemView.findViewById(R.id.btnParticipants);

            btnParticipants.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

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
        holder.tvRideName.setText(ride.getName());
        holder.tvRideOwner.setText(ride.getOwner().displayName());
        holder.tvSrc.setText(ride.getSrc());
        holder.tvDest.setText(ride.getDest());

        setImage(holder.itemView, holder.ivAvatar, ride.getOwner().getPid());
        setImage(holder.itemView, holder.ivRidePic, ride.getPid());
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    private void setImage(View view, ImageView ivAvatar, String pid) {
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);

            Glide.with(view).load(imageRef).into(ivAvatar);
        }
    }


}
