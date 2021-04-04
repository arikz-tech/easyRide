package arik.easyride.adapters;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Objects;

import arik.easyride.R;
import arik.easyride.models.Ride;
import arik.easyride.models.User;
import arik.easyride.models.UserInRide;

//TODO Ripple Effect Accent
public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ViewHolder> {
    private static final String TAG = ".ParticipantsAdapter";
    private List<UserInRide> participants;
    private Ride ride;
    private Activity activity;
    private OnParticipantClick listener;
    private int lastPosition = -1;

    public interface OnParticipantClick {
        void onClick(int index);
    }

    public ParticipantsAdapter(Ride ride, List<UserInRide> participants, Activity activity) {
        this.participants = participants;
        this.activity = activity;
        this.ride = ride;
        listener = (OnParticipantClick) activity;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivLogo;
        TextView tvName, tvArrive;
        CardView cvParticipant;
        ProgressBar pbParticipant;
        Button btnInvite;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvName = itemView.findViewById(R.id.tvName);
            tvArrive = itemView.findViewById(R.id.tvArrive);
            cvParticipant = itemView.findViewById(R.id.cvParticipant);
            pbParticipant = itemView.findViewById(R.id.pbParticipant);
            btnInvite = itemView.findViewById(R.id.btnInvite);

            cvParticipant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = participants.indexOf((UserInRide) itemView.getTag());
                    listener.onClick(index);
                }
            });

            btnInvite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    final int index = participants.indexOf((UserInRide) itemView.getTag());
                    participants.get(index).setInvitationSent(true);
                    notifyItemChanged(index);
                    dbRef.child("rideUsers").child(ride.getRid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                UserInRide participant = snap.getValue(UserInRide.class);
                                if (participant.getUid().equals(participants.get(index).getUid())) {
                                    final String databaseIndex = snap.getKey();
                                    dbRef.child("users").child(participant.getUid()).child("phone").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String phone = snapshot.getValue(String.class);
                                            sendVerificationCode(databaseIndex, phone);
                                            dbRef.child("rideUsers").child(ride.getRid()).child(databaseIndex).child("invitationSent").setValue(true);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, error.getMessage());
                                        }
                                    });


                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, error.getMessage());
                        }
                    });
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
    public void onBindViewHolder(@NonNull final ParticipantsAdapter.ViewHolder holder, int position) {
        int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int colorApprove = 0, colorReject = 0;
        final UserInRide participant = participants.get(position);
        holder.itemView.setTag(participant);
        if (participant.getUid() != null) {
            switch (nightModeFlags) {
                case Configuration.UI_MODE_NIGHT_YES:
                    colorApprove = activity.getColor(R.color.light_green_300);
                    colorReject = activity.getColor(R.color.red_300);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    colorApprove = activity.getColor(R.color.light_green_500);
                    colorReject = activity.getColor(R.color.red_500);
                    break;
            }

            if (participant.isInRide()) {
                holder.cvParticipant.setCardBackgroundColor(colorApprove);
                holder.tvArrive.setText(activity.getText(R.string.arrival_confirmed));
                holder.tvArrive.setVisibility(View.VISIBLE);
                holder.btnInvite.setVisibility(View.GONE);
            } else {
                holder.cvParticipant.setCardBackgroundColor(colorReject);

                if (participant.isContactUser()) {
                    if (isOwner() && !participant.isInvitationSent()) {
                        holder.tvArrive.setVisibility(View.GONE);
                        holder.btnInvite.setVisibility(View.VISIBLE);
                    }else{
                        holder.tvArrive.setText(activity.getText(R.string.arrival_not_confirmed));
                        holder.tvArrive.setVisibility(View.VISIBLE);
                        holder.btnInvite.setVisibility(View.GONE);
                    }

                } else {
                    holder.tvArrive.setText(activity.getText(R.string.arrival_not_confirmed));
                    holder.tvArrive.setVisibility(View.VISIBLE);
                    holder.btnInvite.setVisibility(View.GONE);
                }

            }

            FirebaseDatabase.getInstance().getReference().
                    child("users").child(participant.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    holder.tvName.setText(Objects.requireNonNull(user).displayName());
                    holder.tvName.setTextColor(Color.WHITE);
                    setProfileAvatarFriend(holder, user.getPid());
                    holder.pbParticipant.setVisibility(View.INVISIBLE);
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

    private boolean isOwner() {
        return ride.getOwnerUID().equals(getCurrentUserId());
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

    private void sendVerificationCode(String dbIndex, String phoneNumber) {
        String inviteMessage = activity.getString(R.string.invite_message) + "\"" + ride.getName() + "\"" + "\n";
        String invitationLink = activity.getString(R.string.invitation_link) + "\n" + "https://arikz-tech.github.io/easyrideconfirm?"
                + "rid=" + ride.getRid()
                + "&index=" + dbIndex;
        String allTextMessage = inviteMessage + invitationLink;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("address", phoneNumber);
        intent.putExtra("sms_body", allTextMessage);
        intent.putExtra("exit_on_sent", true);

        activity.startActivity(intent);
    }


}
