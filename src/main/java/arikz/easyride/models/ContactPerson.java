package arikz.easyride.models;

import androidx.annotation.Nullable;

public class ContactPerson {
    private String name;
    private String number;

    public ContactPerson(String name, String number) {
        this.name = name;
        this.number = number;
    }

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
}
