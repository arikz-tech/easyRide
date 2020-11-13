package arikz.easyride.ui.main.rides.add.interfaces;

import com.google.android.gms.maps.model.LatLng;

public interface DetailsEvents {
    void onSubmit(String name, String src, String dest, String date, String pid, LatLng srcLatLng);

    void onImageUpload();

    void onClickAddParticipants();
}
