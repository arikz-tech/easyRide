package arik.easyride.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class ContactPerson implements Parcelable {
    private String name;
    private String number;
    private Uri photo;


    public ContactPerson(String name, String number, Uri photo) {
        this.name = name;
        this.number = number;
        this.photo = photo;
    }

    protected ContactPerson(Parcel in) {
        name = in.readString();
        number = in.readString();
        photo = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<ContactPerson> CREATOR = new Creator<ContactPerson>() {
        @Override
        public ContactPerson createFromParcel(Parcel in) {
            return new ContactPerson(in);
        }

        @Override
        public ContactPerson[] newArray(int size) {
            return new ContactPerson[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ContactPerson) {
            return ((ContactPerson) obj).number.equals(this.number);
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Uri getPhoto() {
        return photo;
    }

    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(number);
        dest.writeParcelable(photo, flags);
    }
}
