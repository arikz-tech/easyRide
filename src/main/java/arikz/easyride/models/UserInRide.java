package arikz.easyride.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UserInRide implements Parcelable {

    private String uid;
    private String latitude;
    private String longitude;
    private boolean inRide;
    private boolean contactUser;
    private boolean invitationSent;

    public UserInRide() {
    }

    protected UserInRide(Parcel in) {
        uid = in.readString();
        latitude = in.readString();
        longitude = in.readString();
        inRide = in.readByte() != 0;
        contactUser = in.readByte() != 0;
        invitationSent = in.readByte() != 0;
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

    public boolean isContactUser() {
        return contactUser;
    }

    public void setContactUser(boolean contactUser) {
        this.contactUser = contactUser;
    }

    public boolean isInvitationSent() {
        return invitationSent;
    }

    public void setInvitationSent(boolean invitationSent) {
        this.invitationSent = invitationSent;
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
        dest.writeByte((byte) (contactUser ? 1 : 0));
        dest.writeByte((byte) (invitationSent ? 1 : 0));
    }
}
