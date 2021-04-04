package arik.easyride.models;


import android.os.Parcel;
import android.os.Parcelable;


public class User implements Parcelable {

    private String uid;
    private String first;
    private String last;
    private String email;
    private String phone;
    private String pid;
    private String address;

    public User() {
    }

    protected User(Parcel in) {
        uid = in.readString();
        first = in.readString();
        last = in.readString();
        email = in.readString();
        phone = in.readString();
        pid = in.readString();
        address = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String displayName() {
        return getFirst() + " " + getLast();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(first);
        dest.writeString(last);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(pid);
        dest.writeString(address);
    }
}
