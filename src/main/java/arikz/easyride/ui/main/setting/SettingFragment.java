package arikz.easyride.ui.main.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.api.AuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.User;
import arikz.easyride.ui.login.LoginActivity;
import arikz.easyride.ui.main.MainActivity;

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
                deleteAccountDialog();
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
                                    if (getActivity() != null) {
                                        MainActivity mainActivity = (MainActivity) getActivity();
                                        mainActivity.updateNavigationBarUserInfo();
                                    }
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
                String newPassword = PasswordInput.getText().toString().trim();
                if (!newPassword.isEmpty()) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        currentUser.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Password has been changed", Toast.LENGTH_SHORT).show();
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

    private void deleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        Context context = getContext();
        assert context != null;
        final GoogleSignInAccount googleUser = GoogleSignIn.getLastSignedInAccount(context);
        boolean isGoogleUser = googleUser != null;

        builder.setTitle(R.string.delete_account);
        builder.setMessage(R.string.sure_delete_acount);

        if (isGoogleUser) {
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String googleTokenId = googleUser.getIdToken();
                    AuthCredential credential = GoogleAuthProvider.getCredential(googleTokenId, null);
                    deleteAccount(credential);
                }
            });
        } else {
            View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.password_dialog_layout, (ViewGroup) getView(), false);
            final TextInputEditText passwordInput = viewInflated.findViewById(R.id.etPassword);
            builder.setView(viewInflated);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    assert passwordInput != null;
                    assert passwordInput.getText() != null;
                    assert FirebaseAuth.getInstance().getCurrentUser() != null;
                    String password = passwordInput.getText().toString().trim();
                    String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    assert currentUserEmail != null;
                    AuthCredential credential = EmailAuthProvider.getCredential(currentUserEmail, password);
                    deleteAccount(credential);
                }
            });
        }

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void deleteAccount(AuthCredential credential) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    String uid = currentUser.getUid();
                                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                                    dbRef.child("users").child(uid).removeValue();
                                    updateParticipatingRides(uid);
                                    FirebaseAuth.getInstance().signOut();

                                    deleteTokenId();

                                    if (getActivity() != null) {
                                        MainActivity mainActivity = (MainActivity) getActivity();
                                        mainActivity.signOut();
                                    }
                                }
                            }
                        });
                    }
                });

    }

    private void deleteTokenId() {
        /*
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        FirebaseAuth.getInstance().getAccessToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult getTokenResult) {
                final String currentUserToken = getTokenResult.getToken();
                final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                dbRef.child("tokens").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String token = snap.getValue(String.class);
                            Log.e(TAG, token);
                            if (currentUserToken.equals(token)) {
                                String key = snap.getKey();
                                Log.e(TAG, key);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });
            }
        });
         */
    }

    private void updateParticipatingRides(String uid) {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("userRides").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String rid = snap.getValue(String.class);
                        assert rid != null;
                        dbRef.child("rides").child(rid).removeValue();
                        dbRef.child("rideUsers").child(rid).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });


    }

}