package arik.easyride.adapters;

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
import androidx.cardview.widget.CardView;
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

import arik.easyride.R;
import arik.easyride.models.User;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
    private List<User> friends;
    private OnFriendClicked friendsFrag;
    private Context context;
    private int lastPosition = -1;

    public interface OnFriendClicked {
        void onClick(int index);
    }

    public FriendsAdapter(List<User> friends, OnFriendClicked friendsFrag, Context context) {
        this.friends = friends;
        this.friendsFrag = friendsFrag;
        this.context = context;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName;
        CardView cvFriend;
        ProgressBar pbFriend;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            cvFriend = itemView.findViewById(R.id.cvFriend);
            pbFriend = itemView.findViewById(R.id.pbFriend);

            cvFriend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = friends.indexOf((User) itemView.getTag());
                    friendsFrag.onClick(index);
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.friends_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User friend = friends.get(position);
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
        return friends.size();
    }

    private void setProfileAvatarFriend(ViewHolder holder, String pid) {

        View view = holder.itemView;
        final ProgressBar pb = holder.pbFriend;
        ImageView ivAvatar = holder.ivAvatar;
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);

            Glide.with(view).load(imageRef).addListener(new RequestListener<Drawable>() {
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

    private void setProfileAvatarContact(ViewHolder holder, String pid) {
        ProgressBar pb = holder.pbFriend;
        if (pid != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(pid));
                Glide.with(context).load(bitmap).into(holder.ivAvatar);
                pb.setVisibility(View.INVISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

