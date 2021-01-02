package arikz.easyride.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import arikz.easyride.R;
import arikz.easyride.models.ContactPerson;
import arikz.easyride.models.User;
import arikz.easyride.ui.main.rides.add.AddContactActivity;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private List<ContactPerson> contactList;
    private Context context;
    private AddContactListener listener;

    public interface AddContactListener {
        void onClick(int index);
    }

    public ContactAdapter(Context context, List<ContactPerson> contactList) {
        this.contactList = contactList;
        this.context = context;
        this.listener = (AddContactListener) context;

    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar;
        MaterialTextView tvName;
        ProgressBar pbParticipant;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            pbParticipant = itemView.findViewById(R.id.pbParticipant);

            MaterialCardView cvParticipant = itemView.findViewById(R.id.cvParticipant);
            cvParticipant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(contactList.indexOf((ContactPerson) itemView.getTag()));
                }
            });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.participants_row_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactPerson contactPerson = contactList.get(position);
        holder.itemView.setTag(contactPerson);
        holder.tvName.setText(contactPerson.getName());
        if (contactPerson.getPhoto() != null) {
            holder.ivAvatar.setImageBitmap(contactPerson.getPhoto());
            holder.pbParticipant.setVisibility(View.INVISIBLE);
        }
        holder.pbParticipant.setVisibility(View.INVISIBLE);

    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }


}
