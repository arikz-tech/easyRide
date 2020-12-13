package arikz.easyride.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UserInRide implements Parcelable {

    private String uid;
    private String latitude;
    private String longitude;
    private boolean inRide;

    public UserInRide() {
    }

    protected UserInRide(Parcel in) {
        uid = in.readString();
        latitude = in.readString();
        longitude = in.readString();
        inRide = in.readByte() != 0;
    }

    public static final Creator<UserInRide> CREATOR = new Creator<UserInRide>() {
        @Override
        public UserInRide createFromParcel(Parcel in) {
            return new UserInRide(in);
        }

        @Override
        public UserInRide[] newArray(int size) {
            return new UserInRide[size];
        }
    };

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public boolean isInRide() {
        return inRide;
    }

    public void setInRide(boolean inRide) {
        this.inRide = inRide;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(latitude);
        dest.writeString(longitude);
        dest.writeByte((byte) (inRide ? 1 : 0));
    }

}
