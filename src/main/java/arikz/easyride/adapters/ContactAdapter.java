package arikz.easyride.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import arikz.easyride.R;
import arikz.easyride.models.ContactPerson;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> implements Filterable {

    private List<ContactPerson> contactList;
    private List<ContactPerson> contactListAll;
    private Context context;
    private AddContactListener listener;

    public interface AddContactListener {
        void onClick(int index);
    }

    public ContactAdapter(Context context, List<ContactPerson> contactList) {
        this.contactList = contactList;
        contactListAll = new ArrayList<>(contactList);
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
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), contactPerson.getPhoto());
                Glide.with(context).load(bitmap).into(holder.ivAvatar);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avatar_logo);
        }
        holder.pbParticipant.setVisibility(View.INVISIBLE);

    }

    public static class ContactPreloadModelProvider implements ListPreloader.PreloadModelProvider<Uri> {
        private ArrayList<ContactPerson> contactList;
        private Context context;

        public ContactPreloadModelProvider(Context context, ArrayList<ContactPerson> contactList) {
            this.contactList = contactList;
            this.context = context;
        }

        @NonNull
        @Override
        public List<Uri> getPreloadItems(int position) {
            Uri uri = contactList.get(position).getPhoto();
            if (uri != null) {
                return Collections.singletonList(uri);
            }
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public RequestBuilder<?> getPreloadRequestBuilder(@NonNull Uri item) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), item);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Glide.with(context).load(bitmap).override(56, 56);
        }
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    @Override
    public Filter getFilter() {
        FilterClass filter = new FilterClass();
        return filter;
    }

    private class FilterClass extends Filter {

        //Run on background thread
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<ContactPerson> filteredList = new ArrayList<>();

            if (constraint.toString().isEmpty()) {
                filteredList.addAll(contactListAll);
            } else {
                for (ContactPerson person : contactListAll) {
                    if (person.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        filteredList.add(person);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;
            return filterResults;
        }

        //Run on UI thread
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            contactList.clear();
            contactList.addAll((Collection<? extends ContactPerson>) results.values);
            notifyDataSetChanged();
        }
    }


}
