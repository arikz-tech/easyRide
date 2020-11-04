package arikz.easyride.ui.main.rides.add.tabs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.User;
import arikz.easyride.ui.main.rides.add.AddParticipantActivity;
import arikz.easyride.ui.main.rides.add.adapters.AddedParticipantsAdapter;
import arikz.easyride.ui.main.rides.add.interfaces.ParticipantsEvents;

public class ParticipantsFragment extends Fragment {
    private static String TAG = ".ParticipantsFragment";
    private static final int ADD_REQUEST_CODE = 17;
    View view;
    List<User> participants;
    AddedParticipantsAdapter participantsAdapter;
    ProgressBar pbParticipants;
    ExtendedFloatingActionButton fabAddParticipant;
    ParticipantsEvents event;

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
        fabAddParticipant = view.findViewById(R.id.fabAddParticipant);

        RecyclerView rvParticipants = view.findViewById(R.id.rvParticipants);
        rvParticipants.setHasFixedSize(true);
        rvParticipants.setLayoutManager(new LinearLayoutManager(getContext()));

        participants = new ArrayList<>();
        participantsAdapter = new AddedParticipantsAdapter(participants);
        rvParticipants.setAdapter(participantsAdapter);

        fabAddParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddParticipantActivity.class);
                intent.putExtra("user", Objects.requireNonNull(Objects.requireNonNull(getActivity()).getIntent().getExtras()).getParcelable("user"));
                startActivityForResult(intent, ADD_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {

                User participant = Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).getParcelable("user");

                //TODO Change this approach !@#
                for (User user : participants)
                    if (user.getUid().equals(Objects.requireNonNull(participant).getUid()))
                        return;


                participants.add(participant);
                participantsAdapter.notifyDataSetChanged();
                event.onAdd(participant);


            }
        }
    }

}