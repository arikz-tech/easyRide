package arikz.easyride.ui.main.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import arikz.easyride.R;
import arikz.easyride.objects.User;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
    List<User> friends;
    OnFriendClicked friendsFrag;

    public interface OnFriendClicked {
        void onClick(int index);
    }

    public FriendsAdapter(List<User> friends, OnFriendClicked friendsFrag) {
        this.friends = friends;
        this.friendsFrag = friendsFrag;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            itemView.setOnClickListener(new View.OnClickListener() {
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

        setProfileAvatar(holder.itemView, holder.ivAvatar, friend.getPid());
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    private void setProfileAvatar(View view, ImageView ivAvatar, String pid) {
        if (pid != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().
                    child("images").child("users").child(pid);

            Glide.with(view).load(imageRef).into(ivAvatar);
        }
    }
}

