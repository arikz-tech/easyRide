package arikz.easyride.ui.main;

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


        PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(Objects.requireNonNull(context));

        while (Objects.requireNonNull(cursor).moveToNext()) {
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String number = formattedPhoneNumber(phoneNumber, phoneUtil);
            phonesList.add(number);
        }

        return phonesList;
    }

    public static String formattedPhoneNumber(String phoneNumber, PhoneNumberUtil phoneUtil) {
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
        if (normalizedNumber.charAt(0) != '+')
            return normalizedNumber;

        try {
            Phonenumber.PhoneNumber number = phoneUtil.parse(normalizedNumber, "");
            long num = number.getNationalNumber();
            String strNum = String.valueOf(num);
            return "0" + strNum;
        } catch (NumberParseException e) {
            e.printStackTrace();
            return normalizedNumber;
        }

    }
}
