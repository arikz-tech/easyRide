package arikz.easyride.ui.main.rides;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import arikz.easyride.R;
import arikz.easyride.data.Ride;
import arikz.easyride.data.User;

public class RidesFragment extends Fragment {
    private static String TAG = ".RidesFragment";
    private static int ADD_REQUEST_CODE = 4;

    private View view;
    private ProgressBar pbRides;
    private RidesAdapter ridesAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_rides, container, false);
        pbRides = view.findViewById(R.id.pbRides);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FloatingActionButton fabAddRide = view.findViewById(R.id.fabAddRide);
        RecyclerView rvRides = view.findViewById(R.id.rvRides);
        rvRides.setHasFixedSize(true);
        rvRides.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Ride> rides = new ArrayList<>();
        ridesAdapter = new RidesAdapter(rides);
        rvRides.setAdapter(ridesAdapter);

        collectRidesInfo(rides);


        fabAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddRideActivity.class);
                startActivityForResult(intent, ADD_REQUEST_CODE);
            }
        });

    }

    private void collectRidesInfo(List<Ride> rides) {
        User user = new User();
        user.setFirst("Arik");
        user.setLast("Zagdon");
        user.setPid("67ac4688-13b5-4140-b1a5-cc7460edc743");
        user.setPhone("0546636137");
        user.setEmail("arikz15@gmail.com");

        Ride ride = new Ride();

        ride.setName("Galil Amaaravi");
        ride.setSrc("Aya 34");
        ride.setDest("Big Karmiel");
        ride.setPid("67ac4688-13b5-4140-b1a5-cc7460edc743");
        ride.setOwner(user);

        rides.add(ride);
        ridesAdapter.notifyDataSetChanged();
        pbRides.setVisibility(View.INVISIBLE);
    }
}