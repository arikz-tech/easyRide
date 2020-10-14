package arikz.easyride.ui.main.requests;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
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

import arikz.easyride.R;
import arikz.easyride.objects.Ride;
import arikz.easyride.objects.User;
import arikz.easyride.objects.UserInRide;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {
    private final static String TAG = ".RequestsAdapter";
    List<Ride> rides;
    Context context;


    public RequestsAdapter(Context context, List<Ride> rides) {
        this.rides = rides;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar;
        MaterialTextView tvRideOwner, tvRideName, tvSrc, tvDest;
        MaterialButton btnConfirm;
        ProgressBar pbConfirm;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            pbConfirm = itemView.findViewById(R.id.pbConfirm);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvRideOwner = itemView.findViewById(R.id.tvRideOwner);
            tvRideName = itemView.findViewById(R.id.tvRideName);
            tvSrc = itemView.findViewById(R.id.tvSrcFill);
            tvDest = itemView.findViewById(R.id.tvDestFill);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                boolean confirm = true;

                @Override
                public void onClick(View v) {
                    int index = rides.indexOf(itemView.getTag());
                    if (confirm) {
                        btnConfirm.setStrokeColorResource(R.color.colorPrimary);
                        btnConfirm.setTextColor(context.getColor(R.color.colorPrimary));
                        btnConfirm.setText(R.string.confirmed);
                    } else {
                        btnConfirm.setStrokeColorResource(R.color.colorBlack);
                        btnConfirm.setTextColor(context.getColor(R.color.colorBlack));
                        btnConfirm.setText(R.string.confirm);
                    }
                    confirmRejectRide(pbConfirm, btnConfirm, index, confirm);
                    confirm = !confirm;
                }
            });

        }
    }

    private void confirmRejectRide(final ProgressBar pb, final MaterialButton btn, int index, final boolean confirm) {
        pb.setVisibility(View.VISIBLE);
        btn.setVisibility(View.INVISIBLE);

        final String rid = rides.get(index).getRid();
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String uid = getCurrentUserId();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    UserInRide user = snap.getValue(UserInRide.class);
                    if (user.getUid().equals(uid)) {
                        String key = snap.getKey();
                        dbRef.child("rideUsers").child(rid).child(key).child("inRide").setValue(confirm).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                pb.setVisibility(View.INVISIBLE);
                                btn.setVisibility(View.VISIBLE);
                            }
                        });
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.requests_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ride ride = rides.get(position);
        holder.itemView.setTag(ride);
        holder.tvRideName.setText(ride.getName());
        holder.tvSrc.setText(ride.getSource());
        holder.tvDest.setText(ride.getDestination());

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

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

}
