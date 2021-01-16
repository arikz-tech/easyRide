package arikz.easyride.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;
import arikz.easyride.models.UserInRide;

//TODO Ripple Effect Accent
public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ViewHolder> {
    private static final String TAG = ".ParticipantsAdapter";
    private List<UserInRide> participants;
    private Context context;
    private OnParticipantClick listener;
    private int lastPosition = -1;

    public interface OnParticipantClick {
        void onClick(int index);
    }

    public ParticipantsAdapter(List<UserInRide> participants, Context context) {
        this.participants = participants;
        this.context = context;
        listener = (OnParticipantClick) context;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName, tvArrive;
        MaterialCardView cvParticipant;
        ProgressBar pbParticipant;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            tvArrive = itemView.findViewById(R.id.tvArrive);
            cvParticipant = itemView.findViewById(R.id.cvParticipant);
            pbParticipant = itemView.findViewById(R.id.pbParticipant);

            cvParticipant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = participants.indexOf((UserInRide) itemView.getTag());
                    listener.onClick(index);
                }
            });
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
        holder.itemView.setTag(participant);

        if (participant.getUid() != null)
            collectUserInfo(holder, participant.getUid(), participant.isInRide(), position);

    }

    private void collectUserInfo(final ViewHolder holder, String uid, boolean inRide, final int position) {

        if (inRide) {
            holder.cvParticipant.setCardBackgroundColor(context.getColor(R.color.colorConfirm));
            holder.tvArrive.setText(context.getText(R.string.arrival_confirmed));
        } else {
            holder.cvParticipant.setCardBackgroundColor(context.getColor(R.color.colorCancel));
            holder.tvArrive.setText(context.getText(R.string.arrival_not_confirmed));
        }

        if (uid != null) {
            FirebaseDatabase.getInstance().getReference().
                    child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    holder.tvName.setText(Objects.requireNonNull(user).displayName());
                    holder.tvName.setTextColor(Color.WHITE);
                    setProfileAvatarFriend(holder, user.getPid());

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    private void setProfileAvatarFriend(ViewHolder holder, String pid) {
        View view = holder.itemView;
        final ProgressBar pb = holder.pbParticipant;
        ImageView ivAvatar = holder.ivAvatar;

        StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                child("images").child("users").child(pid);

        Glide.with(view).load(imageRef).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                pb.setVisibility(View.INVISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                pb.setVisibility(View.INVISIBLE);
                return false;
            }
        }).into(ivAvatar);

    }


}
