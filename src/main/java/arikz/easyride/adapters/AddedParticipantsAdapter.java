package arikz.easyride.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
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
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.List;

import arikz.easyride.R;
import arikz.easyride.models.User;

/**
 * Adapter class,
 * hold RecyclerView items when participant added to the "participant list" while adding new ride.
 * @author Arikz
 * @since 27-03-2021
 */
public class AddedParticipantsAdapter extends RecyclerView.Adapter<AddedParticipantsAdapter.ViewHolder> {

    /**
     * Appropriate context of the application, in order to sync participant images and animation
     */
    private final Context context;

    /**
     * List of participants of particular ride, holds all users information
     */
    private final List<User> participants;

    /**
     *Animation last item position
     */
    private int lastPosition = -1;

    public AddedParticipantsAdapter(List<User> participants, Context context) {
        this.participants = participants;
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName;
        ProgressBar pbParticipant;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            pbParticipant = itemView.findViewById(R.id.pbParticipant);
        }
    }

    @NonNull
    @Override
    public AddedParticipantsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.participants_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddedParticipantsAdapter.ViewHolder holder, int position) {
        User friend = participants.get(position);
        holder.itemView.setTag(friend);
        holder.tvName.setText(friend.displayName());
        if (friend.getEmail() != null) {
            setProfileAvatarFriend(holder, friend.getPid());
        } else {
            setProfileAvatarContact(holder, friend.getPid());
        }
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    private void setProfileAvatarFriend(ViewHolder holder, String pid) {
        View view = holder.itemView;
        final ProgressBar pb = holder.pbParticipant;
        ImageView ivAvatar = holder.ivAvatar;

        if (pid != null) {
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
        } else {
            ivAvatar.setImageResource(R.drawable.avatar_logo);
            pb.setVisibility(View.INVISIBLE);
        }

    }

    private void setProfileAvatarContact(AddedParticipantsAdapter.ViewHolder holder, String pid) {
        if (pid != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(pid));
                Glide.with(context).load(bitmap).into(holder.ivAvatar);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avatar_logo);
        }
        holder.pbParticipant.setVisibility(View.INVISIBLE);
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
