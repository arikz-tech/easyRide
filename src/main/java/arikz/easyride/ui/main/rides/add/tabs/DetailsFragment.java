package arikz.easyride.ui.main.rides.add.tabs;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialDatePicker.Builder;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import arikz.easyride.R;
import arikz.easyride.ui.main.rides.add.interfaces.DetailsEvents;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.app.Activity.RESULT_OK;

public class DetailsFragment extends Fragment {
    private static final String TAG = "DetailsFragment";
    private static final int PERMISSION_REQUEST_CODE = 19;

    private View view;
    private TextInputEditText etName, etSrc, etDest, etDate;
    private ImageView ivRidePic;
    private MaterialButton btnAddRide, btnAddParticipants;
    private DetailsEvents event;
    private FloatingActionButton fabPicEdit;
    private RelativeLayout ivRidePicLayout;
    private ProgressBar pbAddRide;
    private Uri filePath = null;
    private TextInputLayout etNameLayout, etSrcLayout, etDestLayout, etDateLayout;
    private boolean askForPos;
    private LocationManager locationManager;
    private LocationListener listener;

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

        etNameLayout = view.findViewById(R.id.etNameLayout);
        etSrcLayout = view.findViewById(R.id.etSrcLayout);
        etDestLayout = view.findViewById(R.id.etDestLayout);
        etDateLayout = view.findViewById(R.id.etDateLayout);
        ivRidePic = view.findViewById(R.id.ivRidePic);
        etName = view.findViewById(R.id.etName);
        etSrc = view.findViewById(R.id.etSrc);
        etDest = view.findViewById(R.id.etDest);
        etDate = view.findViewById(R.id.etDate);
        btnAddRide = view.findViewById(R.id.btnAddRide);
        fabPicEdit = view.findViewById(R.id.fabPicEdit);
        ivRidePicLayout = view.findViewById(R.id.ivRidePicLayout);
        pbAddRide = view.findViewById(R.id.pbAddRide);
        btnAddParticipants = view.findViewById(R.id.btnAddParticipants);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
            }
        };

        if (ActivityCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 500.0f, listener);

        btnAddRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etName.getText().toString().isEmpty() || etSrc.getText().toString().isEmpty() ||
                        etDest.getText().toString().isEmpty() || etDate.getText().toString().isEmpty())
                    Toast.makeText(getContext(), getString(R.string.enter_fields), Toast.LENGTH_SHORT).show();
                else {
                    String rideName = etName.getText().toString();
                    String source = etSrc.getText().toString();
                    String destination = etDest.getText().toString();
                    String date = etDate.getText().toString();
                    uploadImageAndSubmit(rideName, source, destination, date);
                }
            }
        });

        fabPicEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationManager.removeUpdates(listener);

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(getContext(), DetailsFragment.this);
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
                if (hasFocus && !askForPos) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.current_location);
                    builder.setMessage(R.string.take_current_pos);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            takeUserCurrentPosition();
                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            etSrc.setShowSoftInputOnFocus(true);
                        }
                    }).setNeutralButton(R.string.never_ask_again, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            askForPos = !askForPos;
                            etSrc.setShowSoftInputOnFocus(true);
                        }
                    }).show();

                    locationManager.removeUpdates(listener);
                }
            }
        });

        //TODO TAKE ACTION IN THE ADDRIDEACTIVITY!!
        etDate.setShowSoftInputOnFocus(false);
        etDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
                    builder.setTitleText(R.string.date_select);
                    final MaterialDatePicker materialDatePicker = builder.build();

                    materialDatePicker.show(getActivity().getSupportFragmentManager(), "DATE_PICKER");
                    materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
                        @Override
                        public void onPositiveButtonClick(Object selection) {
                            etDate.setText(materialDatePicker.getHeaderText());
                        }
                    });
                }
            }
        });
    }

    private void takeUserCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            List<Address> addresses;
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                etSrc.setText(addresses.get(0).getAddressLine(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageAndSubmit(final String rideName, final String source, final String destination, final String date) {
        event.onImageUpload();
        pbAddRide.setVisibility(View.VISIBLE);
        ivRidePicLayout.setVisibility(View.INVISIBLE);
        etNameLayout.setVisibility(View.INVISIBLE);
        etSrcLayout.setVisibility(View.INVISIBLE);
        etDestLayout.setVisibility(View.INVISIBLE);
        etDateLayout.setVisibility(View.INVISIBLE);
        btnAddRide.setVisibility(View.INVISIBLE);
        btnAddParticipants.setVisibility(View.INVISIBLE);

        if (filePath != null) {
            final String pid = UUID.randomUUID().toString();
            FirebaseStorage.getInstance().getReference().
                    child("images").child("rides").child(pid).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    event.onSubmit(rideName, source, destination, date, pid);
                }
            });
        } else
            event.onSubmit(rideName, source, destination, date, null);

        locationManager.removeUpdates(listener);

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
            }
        }
    }


}