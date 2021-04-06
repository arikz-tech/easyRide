package arik.easyride.ui.main.rides.add.tabs;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import arik.easyride.R;
import arik.easyride.ui.main.rides.add.interfaces.DetailsEvents;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.app.Activity.RESULT_OK;

public class DetailsFragment extends Fragment {
    private static final String TAG = "DetailsFragment";
    private static final int LOCATION_REQUEST_CODE = 19;

    private View view;
    private TextInputEditText etName, etSrc, etDest;
    private ImageView ivRidePic;
    private MaterialButton btnAddRide, btnAddParticipants;
    private DetailsEvents event; //listener
    private ProgressBar pbAddRide;
    private Chip chipDate, chipTime;
    private String date = "", time = "";
    private Uri filePath = null;
    private LocationManager locationManager;
    private boolean progress = false;

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
        chipDate = view.findViewById(R.id.chipDate);
        chipTime = view.findViewById(R.id.chipTime);
        btnAddRide = view.findViewById(R.id.btnAddRide);
        FloatingActionButton fabPicEdit = view.findViewById(R.id.fabPicEdit);
        pbAddRide = view.findViewById(R.id.pbAddRide);
        btnAddParticipants = view.findViewById(R.id.btnAddParticipants);

        btnAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!progress) {
                    if (!date.isEmpty() && !time.isEmpty()) {
                        if (etName.getText() != null && etSrc.getText() != null && etDest.getText() != null) {
                            if (etName.getText().toString().isEmpty() || etSrc.getText().toString().isEmpty() ||
                                    etDest.getText().toString().isEmpty()) {
                                Toast.makeText(getContext(), getString(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                            } else {
                                String name = etName.getText().toString();
                                String source = etSrc.getText().toString();
                                String destination = etDest.getText().toString();
                                boolean pathValidation = isAddressValid(source) && isAddressValid(destination);
                                if (pathValidation) {
                                    uploadImageAndSubmit(name, source, destination, date, time);
                                } else {
                                    Toast.makeText(getContext(), R.string.could_not_find_location, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), getString(R.string.time_or_date_empty), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.loading), Toast.LENGTH_SHORT).show();
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
                if (etSrc.getText() != null) {
                    if (etSrc.getText().toString().isEmpty()) {
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
                            }).setNegativeButton(R.string.saved_location, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    takeSavedPosition();
                                }
                            }).setNeutralButton(R.string.type, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showKeyboard();
                                }
                            }).show();
                        }
                    } else {
                        showKeyboard();
                    }
                }
            }
        });

        chipDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MaterialDatePicker<Long> datePicker = MaterialDatePicker
                        .Builder
                        .datePicker()
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();
                datePicker.show(getFragmentManager(), "DATE_PICKER");

                datePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                    @Override
                    public void onPositiveButtonClick(Long selection) {
                        date = datePicker.getHeaderText();
                        chipDate.setText(getString(R.string.date_colon) + date);
                    }
                });

            }
        });

        chipTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final MaterialTimePicker timePicker = new MaterialTimePicker.Builder().
                        setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(8)
                        .setMinute(0)
                        .build();

                timePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int newHour = timePicker.getHour();
                        int newMinute = timePicker.getMinute();
                        time = newHour + ":" + (newMinute < 10 ? "0" + newMinute : newMinute);
                        chipTime.setText(getString(R.string.time_colon) + time);
                    }
                });

                timePicker.show(getFragmentManager(), "DATE_PICKER");
            }
        });

    }

    private void showKeyboard() {
        Context context = getContext();
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSrc, InputMethodManager.SHOW_IMPLICIT);
        }

    }

    private void takeSavedPosition() {
        pbAddRide.setVisibility(View.VISIBLE);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String uid = getCurrentUserId();
        class AddressListener implements ValueEventListener {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String address = snapshot.getValue(String.class);
                etSrc.setText(address);
                pbAddRide.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
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
            pbAddRide.setVisibility(View.VISIBLE);
            class Listener implements LocationListener {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    List<Address> addresses;
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(Objects.requireNonNull(location).getLatitude(), location.getLongitude(), 1);
                        etSrc.setText(addresses.get(0).getAddressLine(0));
                        locationManager.removeUpdates(this);
                        pbAddRide.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Listener listener = new Listener();
            locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            }
        }
    }

    private void uploadImageAndSubmit(final String rideName, final String source,
                                      final String destination, final String date, final String time) {
        event.onImageUpload();
        pbAddRide.setVisibility(View.VISIBLE);
        progress = true;
        if (filePath != null) {
            final String pid = UUID.randomUUID().toString();
            FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(pid).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    event.onSubmit(rideName, source, destination, date, time, pid);
                }
            });
        } else
            event.onSubmit(rideName, source, destination, date, time, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                filePath = result.getUri();
                ivRidePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
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

    private boolean isAddressValid(String address) {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            return !addresses.isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}