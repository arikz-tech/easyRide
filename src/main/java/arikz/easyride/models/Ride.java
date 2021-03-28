package arikz.easyride.models;

import android.os.Parcel;
import android.os.Parcelable;


public class Ride implements Parcelable {

    private String rid;
    private String name;
    private String source;
    private String destination;
    private String date;
    private String time;
    private String pid;
    private String ownerUID;

    public Ride(){
    }

    protected Ride(Parcel in) {
        rid = in.readString();
        name = in.readString();
        source = in.readString();
        destination = in.readString();
        date = in.readString();
        time = in.readString();
        pid = in.readString();
        ownerUID = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rid);
        dest.writeString(name);
        dest.writeString(source);
        dest.writeString(destination);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeString(pid);
        dest.writeString(ownerUID);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Ride> CREATOR = new Creator<Ride>() {
        @Override
        public Ride createFromParcel(Parcel in) {
            return new Ride(in);
        }

        @Override
        public Ride[] newArray(int size) {
            return new Ride[size];
        }
    };

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getOwnerUID() {
        return ownerUID;
    }

    public void setOwnerUID(String ownerUID) {
        this.ownerUID = ownerUID;
    }
}
