package arikz.easyride.ui.main.rides.add;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

import arikz.easyride.R;
import arikz.easyride.adapters.ContactAdapter;
import arikz.easyride.models.ContactPerson;
import arikz.easyride.models.User;
import arikz.easyride.util.LoadContacts;

public class AddContactActivity extends AppCompatActivity implements ContactAdapter.AddContactListener {
    private static final int CONTACT_REQUEST_CODE = 15;

    private ContactAdapter contactAdapter;
    private ProgressBar pbContacts;
    private ArrayList<ContactPerson> contactList;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_REQUEST_CODE);
        } else {
            setRecyclerView();
        }
    }

    private void setRecyclerView() {
        LoadContacts loadContacts = new LoadContacts(getApplicationContext());
        contactList = loadContacts.getContactList();
        ListPreloader.PreloadSizeProvider<Uri> sizeProvider =
                new FixedPreloadSizeProvider<>(56, 56);
        ContactAdapter.ContactPreloadModelProvider modelProvider = new ContactAdapter.ContactPreloadModelProvider(getApplicationContext(), contactList);
        RecyclerViewPreloader<Uri> preLoader =
                new RecyclerViewPreloader<>(
                        Glide.with(this), modelProvider, sizeProvider, 10 /*maxPreload*/);

        contactAdapter = new ContactAdapter(this, contactList);
        RecyclerView rvContacts = findViewById(R.id.rvContacts);
        rvContacts.addOnScrollListener(preLoader);
        rvContacts.setAdapter(contactAdapter);
        rvContacts.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
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
        if (contactList.get(index).getPhoto() != null) {
            user.setPid(contactList.get(index).getPhoto().toString());
        }
        data.putExtra("user", user);
        setResult(RESULT_OK, data);
        finish();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                contactAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACT_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setRecyclerView();
            } else
                Toast.makeText(this, R.string.contact_permission__importance, Toast.LENGTH_SHORT).show();
        }
    }
}