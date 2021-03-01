package arikz.easyride.ui.main.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.models.Ride;
import arikz.easyride.models.User;
import arikz.easyride.models.UserInRide;
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

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                sNightMode.setChecked(false);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                sNightMode.setChecked(true);
                break;
        }

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("notificationPref", Context.MODE_PRIVATE);
        boolean mode = sharedPreferences.getBoolean("notificationPref", true);
        if (mode)
            sNotification.setChecked(true);
        else
            sNotification.setChecked(false);

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
                SharedPreferences sharedPreferences = getContext().getSharedPreferences("notificationPref", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (isChecked) {
                    editor.putBoolean("notificationPref", true);
                } else {
                    editor.putBoolean("notificationPref", false);
                }
                editor.apply();
            }
        });

        sNightMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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
        Context context = getContext();
        assert context != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final GoogleSignInAccount googleUser = GoogleSignIn.getLastSignedInAccount(context);
        final boolean isGoogleUser = googleUser != null;
        builder.setTitle(R.string.change_account_email_address);
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.email_dialog_layout, (ViewGroup) getView(), false);
        final EditText emailInput = viewInflated.findViewById(R.id.etMail);
        final EditText passwordInput = viewInflated.findViewById(R.id.etPassword);
        final TextInputLayout textInputLayout = viewInflated.findViewById(R.id.etPasswordLayout);

        if (isGoogleUser) {
            passwordInput.setVisibility(View.GONE);
            textInputLayout.setVisibility(View.GONE);
        }

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isGoogleUser) {
                    String newEmail = emailInput.getText().toString().trim();
                    if (!newEmail.isEmpty()) {
                        String googleTokenId = googleUser.getIdToken();
                        AuthCredential credential = GoogleAuthProvider.getCredential(googleTokenId, null);
                        updateEmail(credential, newEmail);
                    }
                } else {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    assert currentUser != null;
                    String passwordAuth = passwordInput.getText().toString().trim();
                    String newEmail = emailInput.getText().toString().trim();
                    AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), passwordAuth);
                    updateEmail(credential, newEmail);
                }
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
        Context context = getContext();
        assert context != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final GoogleSignInAccount googleUser = GoogleSignIn.getLastSignedInAccount(context);
        final boolean isGoogleUser = googleUser != null;
        builder.setTitle(R.string.change_login_password);
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.password_dialog_layout, (ViewGroup) getView(), false);
        final EditText oldPasswordInput = viewInflated.findViewById(R.id.etOldPassword);
        final EditText newPasswordInput = viewInflated.findViewById(R.id.etNewPassword);
        final TextInputLayout oldPasswordLayout = viewInflated.findViewById(R.id.etOldPasswordLayout);

        if (isGoogleUser) {
            oldPasswordInput.setVisibility(View.GONE);
            oldPasswordLayout.setVisibility(View.GONE);
        }

        builder.setView(viewInflated);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isGoogleUser) {
                    String newPassword = newPasswordInput.getText().toString().trim();
                    if (!newPassword.isEmpty()) {
                        String googleTokenId = googleUser.getIdToken();
                        AuthCredential credential = GoogleAuthProvider.getCredential(googleTokenId, null);
                        updatePassword(credential, newPassword);
                    }
                } else {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    assert currentUser != null;
                    String passwordAuth = oldPasswordInput.getText().toString().trim();
                    String newEmail = newPasswordInput.getText().toString().trim();
                    AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), passwordAuth);
                    updatePassword(credential, newEmail);
                }
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
        Context context = getContext();
        assert context != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
            final TextInputEditText newPasswordInput = viewInflated.findViewById(R.id.etNewPassword);
            final TextInputEditText passwordInput = viewInflated.findViewById(R.id.etOldPassword);
            final TextInputLayout newPasswordLayout = viewInflated.findViewById(R.id.etNewPasswordLayout);

            newPasswordInput.setVisibility(View.GONE);
            newPasswordLayout.setVisibility(View.GONE);

            builder.setView(viewInflated);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String password = passwordInput.getText().toString().trim();
                    String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    if (!password.isEmpty() && !currentUserEmail.isEmpty()) {
                        AuthCredential credential = EmailAuthProvider.getCredential(currentUserEmail, password);
                        deleteAccount(credential);
                    } else {
                        Toast.makeText(getContext(), "Wrong Input", Toast.LENGTH_SHORT).show();
                    }
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
                        if (task.isSuccessful()) {
                            currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        String uid = currentUser.getUid();
                                        deleteUserStorageAndDatabaseInfo(uid);

                                    }
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void deleteUserStorageAndDatabaseInfo(final String uid) {
        FirebaseDatabase
                .getInstance()
                .getReference()
                .child("users")
                .child(uid)
                .child("pid")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String pid = snapshot.getValue(String.class);
                        FirebaseStorage.getInstance()
                                .getReference()
                                .child("images")
                                .child("users")
                                .child(pid)
                                .delete();

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

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });
    }

    private void updateEmail(AuthCredential credential, final String newEmailAddress) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            currentUser.updateEmail(newEmailAddress).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        String uid = currentUser.getUid();
                                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                                        dbRef.child("users").child(uid).child("email").setValue(newEmailAddress);
                                        Toast.makeText(getContext(), "Email as been updated to " + newEmailAddress, Toast.LENGTH_SHORT).show();
                                        if (getActivity() != null) {
                                            MainActivity mainActivity = (MainActivity) getActivity();
                                            mainActivity.updateNavigationBarUserInfo();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword(AuthCredential credential, final String newPassword) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            currentUser.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "The password Has been changed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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

    private void updateParticipatingRides(final String uid) {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("userRides")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String rid = snap.getValue(String.class);
                                snap.getRef().removeValue();
                                assert rid != null;
                                dbRef.child("rides").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Ride ride = snapshot.getValue(Ride.class);
                                        final String rid = ride.getRid();
                                        if (ride.getOwnerUID().equals(uid)) {
                                            snapshot.getRef().removeValue();
                                            dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                                        UserInRide userInRide = snap.getValue(UserInRide.class);
                                                        dbRef.child("users").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                User user = snapshot.getValue(User.class);
                                                                if (user != null) {
                                                                    if (user.getEmail() == null) {
                                                                        FirebaseStorage
                                                                                .getInstance()
                                                                                .getReference()
                                                                                .child("images")
                                                                                .child("users")
                                                                                .child(user.getPid())
                                                                                .delete();
                                                                        snapshot.getRef().removeValue();
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                                Log.e(TAG, error.getMessage());
                                                            }
                                                        });

                                                        dbRef.child("userRides").child(userInRide.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot snap : snapshot.getChildren()) {
                                                                    String userRidesRid = snap.getValue(String.class);
                                                                    if (userRidesRid.equals(rid)) {
                                                                        snap.getRef().removeValue();
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                                Log.e(TAG, error.getMessage());
                                                            }
                                                        });
                                                    }
                                                    snapshot.getRef().removeValue();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e(TAG, error.getMessage());
                                                }
                                            });

                                            if (ride.getPid() != null) {
                                                FirebaseStorage
                                                        .getInstance()
                                                        .getReference()
                                                        .child("images")
                                                        .child("rides")
                                                        .child(ride.getPid())
                                                        .delete();
                                            }


                                        } else {
                                            dbRef.child("rideUsers").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                                        UserInRide userInRide = snap.getValue(UserInRide.class);
                                                        if (userInRide.getUid().equals(uid)) {
                                                            snap.getRef().removeValue();
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

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, error.getMessage());
                                    }
                                });

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