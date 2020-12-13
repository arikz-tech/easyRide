package arikz.easyride.util;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;


public class LoadContacts {

    Context context;

    public LoadContacts(Context context) {
        this.context = context;

    }

    public List<String> getContactsPhoneNumbers() {

        List<String> phonesList = new ArrayList<>();

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        ContentResolver resolver = Objects.requireNonNull(context).getContentResolver(); // check if activity needed
        Cursor cursor = resolver.query(uri, projection, null, null, null);

        while (Objects.requireNonNull(cursor).moveToNext()) {
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String number = formattedPhoneNumber(phoneNumber);
            phonesList.add(number);
        }

        return phonesList;
    }

    public static String formattedPhoneNumber(String phoneNumber) {
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);

        if (normalizedNumber.contains("+972"))
            return "0" + normalizedNumber.substring(4);

        return normalizedNumber;
    }
}
