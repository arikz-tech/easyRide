package arikz.easyride.ui.main.rides.add.tabs;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import arikz.easyride.R;
import arikz.easyride.ui.main.rides.add.interfaces.DetailsEvents;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class DetailsFragment extends Fragment {
    private static final String TAG = "DetailsFragment";
    private static final int LOCATION_REQUEST_CODE = 19;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 79;

    private View view;
    private TextInputEditText etName, etSrc, etDest, etDate;
    private ImageView ivRidePic;
    private MaterialButton btnAddRide, btnAddParticipants;
    private DetailsEvents event;
    private ProgressBar pbAddRide, pbLocation;
    private Uri filePath = null;

    public DetailsFragment(Context context) {
        event = (DetailsEvents) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_details, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ivRidePic = view.findViewById(R.id.ivRidePic);
        etName = view.findViewById(R.id.etName);
        etSrc = view.findViewById(R.id.etSrc);
        etDest = view.findViewById(R.id.etDest);
        etDate = view.findViewById(R.id.etDate);
        btnAddRide = view.findViewById(R.id.btnAddRide);
        FloatingActionButton fabPicEdit = view.findViewById(R.id.fabPicEdit);
        pbAddRide = view.findViewById(R.id.pbAddRide);
        pbLocation = view.findViewById(R.id.pbLocation);
        btnAddParticipants = view.findViewById(R.id.btnAddParticipants);

        btnAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Objects.requireNonNull(etName.getText()).toString().isEmpty() || Objects.requireNonNull(etSrc.getText()).toString().isEmpty() ||
                        Objects.requireNonNull(etDest.getText()).toString().isEmpty() || Objects.requireNonNull(etDate.getText()).toString().isEmpty())
                    Toast.makeText(getContext(), getString(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                else {
                    String rideName = etName.getText().toString();
                    String source = etSrc.getText().toString();
                    String destination = etDest.getText().toString();
                    String date = etDate.getText().toString();
                    LatLng srcLatLng = getLocationFromAddress(getContext(), source);
                    if (srcLatLng != null)
                        uploadImageAndSubmit(rideName, source, destination, date, srcLatLng);
                }
            }
        });

        fabPicEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(Objects.requireNonNull(getContext()), DetailsFragment.this);
            }
        });

        btnAddParticipants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event.onClickAddParticipants();
            }
        });

        etSrc.setShowSoftInputOnFocus(false);
        etSrc.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.starting_point);
                    builder.setMessage(R.string.wich_location);
                    builder.setIcon(R.drawable.ic_start_flag_24);
                    builder.setPositiveButton(R.string.current_location, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            takeUserCurrentPosition();
                        }
                    }).setNegativeButton(R.string.my_address, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            takeSavedPosition();
                        }
                    }).setNeutralButton(R.string.type, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            etSrc.setShowSoftInputOnFocus(true);
                        }
                    }).show();
                }
            }
        });

        etDate.setShowSoftInputOnFocus(false);
        etDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            private int hour;
            private int minutes;
            private int clockFormat;

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    clockFormat = TimeFormat.CLOCK_24H;

                    class OnTimeClicked implements View.OnClickListener {
                        private MaterialTimePicker materialTimePicker;
                        private String date;

                        public OnTimeClicked(MaterialTimePicker materialTimePicker, String date) {
                            this.date = date;
                            this.materialTimePicker = materialTimePicker;
                        }

                        @Override
                        public void onClick(View v) {
                            int newHour = materialTimePicker.getHour();
                            int newMinute = materialTimePicker.getMinute();
                            String time = newHour + ":" + (newMinute < 10 ? "0" + newMinute : newMinute);
                            String completeDate = date + ", " + time;
                            etDate.setText(completeDate);
                        }
                    }

                    class OnDateClicked implements MaterialPickerOnPositiveButtonClickListener<Long> {
                        private MaterialDatePicker<Long> materialDatePicker;

                        public OnDateClicked(MaterialDatePicker<Long> materialDatePicker) {
                            this.materialDatePicker = materialDatePicker;
                        }

                        @Override
                        public void onPositiveButtonClick(Long selection) {
                            MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder().
                                    setTimeFormat(clockFormat)
                                    .setHour(hour)
                                    .setMinute(minutes)
                                    .build();

                            OnTimeClicked timeListener = new OnTimeClicked(materialTimePicker, materialDatePicker.getHeaderText());
                            materialTimePicker.addOnPositiveButtonClickListener(timeListener);
                            materialTimePicker.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "TIME_PICKER");
                        }
                    }

                    MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                    builder.setTitleText(R.string.date_select);
                    MaterialDatePicker<Long> materialDatePicker = builder.build();

                    materialDatePicker.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "DATE_PICKER");
                    OnDateClicked dateListener = new OnDateClicked(materialDatePicker);
                    materialDatePicker.addOnPositiveButtonClickListener(dateListener);
                }
            }
        });

    }

    private void takeSavedPosition() {
        pbLocation.setVisibility(View.VISIBLE);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String uid = getCurrentUserId();
        class AddressListener implements ValueEventListener {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String address = snapshot.getValue(String.class);
                etSrc.setText(address);
                pbLocation.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }

        AddressListener addressListener = new AddressListener();
        dbRef.child("users").child(Objects.requireNonNull(uid)).child("address").addListenerForSingleValueEvent(addressListener);

    }

    private void takeUserCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            final LocationManager locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
            pbLocation.setVisibility(View.VISIBLE);
            class Listener implements LocationListener {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    List<Address> addresses;
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(Objects.requireNonNull(location).getLatitude(), location.getLongitude(), 1);
                        etSrc.setText(addresses.get(0).getAddressLine(0));
                        locationManager.removeUpdates(this);
                        pbLocation.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Listener listener = new Listener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5, listener);
        }
    }

    private void uploadImageAndSubmit(final String rideName, final String source,
                                      final String destination, final String date, final LatLng srcLatLng) {
        event.onImageUpload();
        btnAddRide.setVisibility(View.INVISIBLE);
        btnAddParticipants.setVisibility(View.INVISIBLE);
        pbAddRide.setVisibility(View.VISIBLE);
        if (filePath != null) {
            final String pid = UUID.randomUUID().toString();
            FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(pid).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    event.onSubmit(rideName, source, destination, date, pid, srcLatLng);
                }
            });
        } else
            event.onSubmit(rideName, source, destination, date, null, srcLatLng);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                filePath = result.getUri();

                Glide.with(this).load(filePath).into(ivRidePic);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(getActivity(), result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                Exception error = result.getError();
                Log.d(TAG, error + "");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takeUserCurrentPosition();
            }
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);

            if (address == null) {
                Toast.makeText(context, R.string.start_point_not_found, Toast.LENGTH_SHORT).show();
                return null;
            }

            if (address.isEmpty()) {
                Toast.makeText(context, R.string.start_point_not_found, Toast.LENGTH_SHORT).show();
                return null;
            } else {
                Address location = address.get(0);
                p1 = new LatLng(location.getLatitude(), location.getLongitude());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return p1;
    }

}