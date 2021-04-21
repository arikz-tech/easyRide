package arik.easyride.util;

import android.content.Context;

import com.google.type.LatLng;

import java.util.List;

import arik.easyride.models.UserInRide;

public class Stations {
    private Context context;
    private List<LatLng> stations;
    private List<UserInRide> participants;

    public Stations(Context context, List<UserInRide> participants) {
        this.context = context;
        this.participants = participants;
    }
}
