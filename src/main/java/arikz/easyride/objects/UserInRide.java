package arikz.easyride.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class UserInRide implements Parcelable {

    private String uid;
    private boolean inRide;
    private double latitude;
    private double longitude;

    public UserInRide(){
    }

    protected UserInRide(Parcel in) {
        uid = in.readString();
        inRide = in.readByte() != 0;
        latitude = in.readDouble();
        longitude = in.readDouble();
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

    public boolean isInRide() {
        return inRide;
    }

    public void setInRide(boolean inRide) {
        this.inRide = inRide;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeByte((byte) (inRide ? 1 : 0));
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
