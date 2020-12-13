package arikz.easyride.ui.main.rides;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.Ride;
import arikz.easyride.ui.main.rides.info.RideInfoActivity;
import arikz.easyride.util.LoadData;
import arikz.easyride.ui.main.rides.add.AddRideActivity;
import arikz.easyride.adapters.RidesAdapter;

public class RidesFragment extends Fragment implements RidesAdapter.OnRideClicked {
    private static String TAG = ".RidesFragment";
    private static int ADD_REQUEST_CODE = 4;
    private static int LEAVE_REQUEST_CODE = 7;

    private View view;
    private ImageView ivNoRides;
    private MaterialTextView tvNoRides;
    private ProgressBar pbRides;
    private RidesAdapter ridesAdapter; //Adapter that holds ride items for recyclerview
    private List<Ride> rides; //Hold rides data

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflate fragment ride layout, insert
        view = inflater.inflate(R.layout.fragment_rides, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ivNoRides = view.findViewById(R.id.ivNoRides);
        tvNoRides = view.findViewById(R.id.tvNoRides);
        pbRides = view.findViewById(R.id.pbRides);
        ExtendedFloatingActionButton fabAddRide = view.findViewById(R.id.fabAddRide);

        RecyclerView rvRides = view.findViewById(R.id.rvRides);
        rvRides.setHasFixedSize(true);
        rvRides.setLayoutManager(new LinearLayoutManager(getContext()));

        rides = new ArrayList<>();
        ridesAdapter = new RidesAdapter(getContext(), this, rides);
        rvRides.setAdapter(ridesAdapter);

        LoadData loadRides = new LoadData();
        loadRides.setItemsList(rides);
        loadRides.setRidesAdapter(ridesAdapter);
        loadRides.setProgressBar(pbRides);
        loadRides.setIvNoData(ivNoRides);
        loadRides.setTvNoData(tvNoRides);
        loadRides.load();

        fabAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getArguments() != null) {
                    Intent intent = new Intent(getActivity(), AddRideActivity.class);
                    intent.putExtra("user", getArguments().getParcelable("user"));
                    startActivityForResult(intent, ADD_REQUEST_CODE);
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Ride ride = Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).getParcelable("ride");
                rides.add(ride);
                ridesAdapter.notifyDataSetChanged();
                tvNoRides.setVisibility(View.INVISIBLE);
                ivNoRides.setVisibility(View.INVISIBLE);
            }
        }

        if (requestCode == LEAVE_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                String rid = Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).getString("ride");

                if (!rides.isEmpty()) {
                    for (Ride ride : rides) {
                        if (ride.getRid().equals(rid)) {
                            rides.remove(ride);
                            break;
                        }
                    }
                }

                if(rides.isEmpty()){
                    ivNoRides.setVisibility(View.VISIBLE);
                    tvNoRides.setVisibility(View.VISIBLE);
                }

                ridesAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onClick(int index) {
        Intent intent = new Intent(getActivity(), RideInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("ride", rides.get(index));
        startActivityForResult(intent, LEAVE_REQUEST_CODE);
    }
}