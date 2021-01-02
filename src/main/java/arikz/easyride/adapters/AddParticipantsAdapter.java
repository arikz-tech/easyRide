package arikz.easyride.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import arikz.easyride.R;
import arikz.easyride.models.User;

//TODO Ripple Effect Accent
public class AddParticipantsAdapter extends RecyclerView.Adapter<AddParticipantsAdapter.ViewHolder> {
    private static final String TAG = ".AddParticipantsAdapter";
    private Context activity;
    private List<User> participants;
    private AddParticipantListener clickHandle;
    private int lastPosition = -1;

    public interface AddParticipantListener {
        void onClick(int index);
    }

    public AddParticipantsAdapter(List<User> participants, Context activity) {
        this.participants = participants;
        clickHandle = (AddParticipantListener) activity;
        this.activity = activity;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName;
        MaterialCardView cvParticipant;
        ProgressBar pbParticipant;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            cvParticipant = itemView.findViewById(R.id.cvParticipant);
            pbParticipant = itemView.findViewById(R.id.pbParticipant);

            cvParticipant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickHandle.onClick(participants.indexOf((User) itemView.getTag()));
                }
            });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.participants_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User friend = participants.get(position);
        holder.itemView.setTag(friend);
        holder.tvName.setText(friend.displayName());
        setProfileAvatar(holder,friend.getPid());
        setAnimation(holder.itemView,position);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    private void setProfileAvatar(ViewHolder holder ,String pid) {
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

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

}
