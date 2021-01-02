package arikz.easyride.ui.main.rides.add;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import arikz.easyride.R;
import arikz.easyride.adapters.ContactAdapter;
import arikz.easyride.adapters.FriendsAdapter;
import arikz.easyride.models.ContactPerson;
import arikz.easyride.models.User;
import arikz.easyride.util.LoadContacts;

public class AddContactActivity extends AppCompatActivity implements LoadContacts.CompleteListener, ContactAdapter.AddContactListener {
    private ContactAdapter contactAdapter;
    private ProgressBar pbContacts;
    private ArrayList<ContactPerson> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        pbContacts = findViewById(R.id.pbContacts);
        contactList = new ArrayList<>();
        contactAdapter = new ContactAdapter(this, contactList);
        LoadContacts loadContacts = new LoadContacts(getApplicationContext(), contactList, this);
        loadContacts.start();
        RecyclerView rvContacts = findViewById(R.id.rvContacts);
        rvContacts.setAdapter(contactAdapter);
        rvContacts.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @Override
    public void finishedCallback() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                contactAdapter.notifyDataSetChanged();
                pbContacts.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onClick(int index) {
        Intent data = new Intent();
        User user = new User();
        String first = contactList.get(index).getName();
        String last = "";
        String phone = contactList.get(index).getNumber();
        user.setFirst(first);
        user.setLast(last);
        user.setPhone(phone);
        user.setPid("no_image_avatar.png");
        data.putExtra("user", user);
        setResult(RESULT_OK, data);
        finish();
    }
}