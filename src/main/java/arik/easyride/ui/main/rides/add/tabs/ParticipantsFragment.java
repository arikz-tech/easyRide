package arik.easyride.ui.main.rides.add.tabs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arik.easyride.R;
import arik.easyride.models.User;
import arik.easyride.ui.main.rides.add.AddContactActivity;
import arik.easyride.ui.main.rides.add.AddParticipantActivity;
import arik.easyride.adapters.AddedParticipantsAdapter;
import arik.easyride.ui.main.rides.add.interfaces.ParticipantsEvents;

public class ParticipantsFragment extends Fragment {
    private static String TAG = ".ParticipantsFragment";
    private static final int ADD_REQUEST_CODE = 17;

    private View view;
    private List<User> participants;
    private AddedParticipantsAdapter participantsAdapter;
    private ProgressBar pbParticipants;
    private ExtendedFloatingActionButton fabAddParticipant, fabAddPhone, fabAddContact;
    private FloatingActionButton fabAdd;
    private ParticipantsEvents event;
    private Animation fabOpen, fabClose, fabOpenRotate, fabCloseRotate;
    private boolean isOpen = false;

    public ParticipantsFragment(Context context) {
        event = (ParticipantsEvents) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_participants, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        pbParticipants = view.findViewById(R.id.pbParticipants);
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAddParticipant = view.findViewById(R.id.fabAddFriend);
        fabAddPhone = view.findViewById(R.id.fabAddPhone);
        fabAddContact = view.findViewById(R.id.fabAddContact);

        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_anim);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_anim);
        fabOpenRotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_open_anim);
        fabCloseRotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_close_anim);

        fabOpen.setDuration(200);
        fabClose.setDuration(200);
        fabOpenRotate.setDuration(200);
        fabCloseRotate.setDuration(200);

        RecyclerView rvParticipants = view.findViewById(R.id.rvParticipants);
        rvParticipants.setHasFixedSize(true);
        rvParticipants.setLayoutManager(new LinearLayoutManager(getContext()));

        participants = new ArrayList<>();
        participantsAdapter = new AddedParticipantsAdapter(participants, getContext());
        rvParticipants.setAdapter(participantsAdapter);

        ItemTouchControl touchControl = new ItemTouchControl(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchControl);
        itemTouchHelper.attachToRecyclerView(rvParticipants);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpen) {
                    fabAddParticipant.startAnimation(fabClose);
                    fabAddPhone.startAnimation(fabClose);
                    fabAddContact.startAnimation(fabClose);
                    fabAdd.startAnimation(fabCloseRotate);

                    fabAddPhone.setClickable(false);
                    fabAddParticipant.setClickable(false);
                    fabAddContact.setClickable(false);
                    isOpen = false;
                } else {
                    fabAddParticipant.startAnimation(fabOpen);
                    fabAddPhone.startAnimation(fabOpen);
                    fabAddContact.startAnimation(fabOpen);
                    fabAdd.startAnimation(fabOpenRotate);

                    fabAddPhone.setClickable(true);
                    fabAddParticipant.setClickable(true);
                    fabAddContact.setClickable(true);
                    isOpen = true;
                }
            }
        });

        fabAddParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddParticipantActivity.class);
                startActivityForResult(intent, ADD_REQUEST_CODE);
            }
        });

        fabAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    addContact();
            }
        });

        fabAddPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    addPhoneDialog();
            }
        });
    }

    private void addContact() {
        Intent intent = new Intent(getActivity(), AddContactActivity.class);
        startActivityForResult(intent, ADD_REQUEST_CODE);
    }

    private class ItemTouchControl extends ItemTouchHelper.SimpleCallback {

        public ItemTouchControl(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            participants.remove((User) viewHolder.itemView.getTag());
            event.onRemove((User) viewHolder.itemView.getTag());
            participantsAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
        }
    }

    private void addPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.add_phone_number);
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.phone_dialog_layout, (ViewGroup) getView(), false);
        final EditText NameInput = viewInflated.findViewById(R.id.etName);
        final EditText PhoneInput = viewInflated.findViewById(R.id.etPhone);
        final AutoCompleteTextView etArea = viewInflated.findViewById(R.id.etArea);

        String[] areas = {"050", "051", "052", "053","054"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item, areas);
        etArea.setAdapter(adapter);

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = NameInput.getText().toString().trim();
                String phone = etArea.getText().toString().trim() + PhoneInput.getText().toString().trim();
                if (!name.isEmpty() && !phone.isEmpty()) {
                    User phoneUser = new User();
                    phoneUser.setFirst(name);
                    phoneUser.setLast("");
                    phoneUser.setPhone(phone);
                    phoneUser.setPid("avatar_logo.png");
                    participants.add(phoneUser);
                    participantsAdapter.notifyDataSetChanged();
                    event.onAdd(phoneUser);
                } else
                    Toast.makeText(getContext(), R.string.enter_fields, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                User participant = Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).getParcelable("user");

                //TODO Change this approach !@#
                for (User user : participants)
                    if (user.getUid() != null)
                        if (user.getUid().equals(Objects.requireNonNull(participant).getUid()))
                            return;

                participants.add(participant);
                participantsAdapter.notifyDataSetChanged();
                event.onAdd(participant);
            }
        }
    }

}