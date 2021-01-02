package arikz.easyride.util;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arikz.easyride.adapters.ContactAdapter;
import arikz.easyride.models.ContactPerson;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;


public class LoadContacts extends Thread {

    Context context;
    ArrayList<ContactPerson> contactList;
    CompleteListener listener;

    public interface CompleteListener {
        void finishedCallback();
    }

    public LoadContacts(Context context, ArrayList<ContactPerson> contactList, CompleteListener listener) {
        this.context = context;
        this.contactList = contactList;
        this.listener = listener;
    }

    @Override
    public void run() {
        super.run();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Photo.PHOTO_URI};
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String number = formattedPhoneNumber(phoneNumber);
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                if (photoUri != null) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(photoUri));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    contactList.add(new ContactPerson(name, number, bitmap));
                } else {
                    contactList.add(new ContactPerson(name, number));
                }
            }
            cursor.close();
            listener.finishedCallback();
        }

    }

    public static String formattedPhoneNumber(String phoneNumber) {
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);

        if (normalizedNumber.contains("+972"))
            return "0" + normalizedNumber.substring(4);

        return normalizedNumber;
    }

}
