package arikz.easyride.ui.main.rides.add.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
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
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;

//TODO Ripple Effect Accent
public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ViewHolder> {
    private static final String TAG = ".ParticipantsAdapter";
    List<UserInRide> participants;
    Context activity;
    OnParticipantClick clickHandle;

    public interface OnParticipantClick {
        void onClick(int index);
    }

    public ParticipantsAdapter(List<UserInRide> participants, Context activity) {
        this.participants = participants;
        this.activity = activity;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName,tvArrive;
        MaterialCardView cvRow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            tvArrive = itemView.findViewById(R.id.tvArrive);
            cvRow = itemView.findViewById(R.id.cvRow);
        }
    }

    @NonNull
    @Override
    public ParticipantsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.participants_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantsAdapter.ViewHolder holder, int position) {
        UserInRide participant = participants.get(position);
        collectUserInfo(holder,participant.getUid(),participant.isInRide());
    }

    private void collectUserInfo(final ViewHolder holder ,String uid,boolean inRide) {

        if(inRide){
            holder.cvRow.setCardBackgroundColor(activity.getColor(R.color.colorConfirm));
            holder.tvArrive.setText(activity.getText(R.string.arrival_confirmed));
        }else{
            holder.cvRow.setCardBackgroundColor(activity.getColor(R.color.colorCancel));
            holder.tvArrive.setText(activity.getText(R.string.arrival_not_confirmed));
        }

        FirebaseDatabase.getInstance().getReference().
                child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                holder.tvName.setText(Objects.requireNonNull(user).displayName());
                holder.tvName.setTextColor(Color.WHITE);
                setProfileAvatar(holder.itemView, holder.ivAvatar, user.getPid());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });


    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    private void setProfileAvatar(View view, ImageView ivAvatar, String pid) {
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);

            Glide.with(view).load(imageRef).into(ivAvatar);
        }
    }
}
