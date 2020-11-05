package arikz.easyride.ui.main.rides.adapters;

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

//TODO Ripple Effect Accent
public class AddedParticipantsAdapter extends RecyclerView.Adapter<AddedParticipantsAdapter.ViewHolder> {
    private static final String TAG = ".ParticipantsAdapter";
    List<User> participants;
    OnParticipantClick clickHandle;

    public interface OnParticipantClick {
        void onClick(int index);
    }

    public AddedParticipantsAdapter( List<User> participants) {
        this.participants = participants;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivLogo;
        MaterialTextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
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

        setProfileAvatar(holder.itemView, holder.ivAvatar, friend.getPid());
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
