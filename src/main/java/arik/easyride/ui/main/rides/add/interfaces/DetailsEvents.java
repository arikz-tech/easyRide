package arik.easyride.ui.main.rides.add.interfaces;

public interface DetailsEvents {
    void onSubmit(String name, String src, String dest, String date, String time,int numberOfStations, String pid);

    void onImageUpload();

    void onClickAddParticipants();
}
