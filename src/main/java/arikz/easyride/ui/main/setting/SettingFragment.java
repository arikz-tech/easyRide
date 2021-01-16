package arikz.easyride.ui.main.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;

public class SettingFragment extends Fragment {
    private static final String TAG = ".SettingFragment";
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_setting, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MaterialCardView cvEmailChange = view.findViewById(R.id.cvEmailChange);
        MaterialCardView cvPasswordChange = view.findViewById(R.id.cvPasswordChange);
        MaterialCardView cvDeleteAccount = view.findViewById(R.id.cvDeleteAccount);
        MaterialTextView tvVersion = view.findViewById(R.id.tvVersion);
        SwitchMaterial sNotification = view.findViewById(R.id.sNotification);
        SwitchMaterial sNightMode = view.findViewById(R.id.sNightMode);

        cvEmailChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailAddressChangeDialog();
            }
        });

        cvPasswordChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordChangeDialog();
            }
        });

        cvDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });

        sNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getContext(), "ON", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });


        sNightMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getContext(), "ON", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });

        try {
            Context context = getContext();
            if (context != null) {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String version = pInfo.versionName;
                tvVersion.setText(version);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void emailAddressChangeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.change_account_email_address);
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.email_dialog_layout, (ViewGroup) getView(), false);

        final EditText EmailInput = viewInflated.findViewById(R.id.etMail);
        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String newEmail = EmailInput.getText().toString().trim();
                if (!newEmail.isEmpty()) {
                    final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        currentUser.updateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    String uid = currentUser.getUid();
                                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                                    dbRef.child("users").child(uid).child("email").setValue(newEmail);
                                    Toast.makeText(getContext(), "Email as been updated to" + newEmail, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else
                    Toast.makeText(getContext(), R.string.enter_fields, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void passwordChangeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.change_login_password);
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.password_dialog_layout, (ViewGroup) getView(), false);

        final EditText PasswordInput = viewInflated.findViewById(R.id.etPassword);
        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = PasswordInput.getText().toString().trim();
                if (!password.isEmpty()) {
                    Toast.makeText(getContext(), password, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getContext(), R.string.enter_fields, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void deleteAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.delete_account);
        builder.setMessage(R.string.sure_delete_acount);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.show();
    }

}