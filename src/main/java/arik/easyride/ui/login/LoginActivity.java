package arik.easyride.ui.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import arik.easyride.models.User;
import arik.easyride.models.UserInRide;
import arik.easyride.ui.main.MainActivity;
import arik.easyride.R;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = ".LoginActivity";

    //Constant variables
    private static final int RC_SIGN_IN = 3; //Request code for google sign in
    private static final int REGISTER_INFO = 5; //
    private static final int PHONE_NUMBER_PERMISSION = 20;

    private TextInputEditText etMail, etPassword;
    private ProgressBar pbLogin;
    private GoogleSignInAccount account;

    @Override
    protected void onStart() {
        super.onStart();
        login();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Attach layout component
        pbLogin = findViewById(R.id.pbLogin);
        etMail = findViewById(R.id.etMail);
        etPassword = findViewById(R.id.etPassword);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        SignInButton btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        MaterialTextView tvForgot = findViewById(R.id.tvForgot);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);

        //Change google button size to wide
        btnGoogleLogin.setSize(SignInButton.SIZE_WIDE);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailSignIn(v);
            }
        });

        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn(v);
            }

        });

        tvForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, PasswordResetActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(intent, REGISTER_INFO);
            }
        });

    }

    private void emailSignIn(final View view) {
        boolean emailField = Objects.requireNonNull(etMail.getText()).toString().isEmpty();
        boolean passField = Objects.requireNonNull(etPassword.getText()).toString().isEmpty();

        if (emailField)
            etMail.setError(getString(R.string.incorrect_email_adress));

        if (passField)
            etPassword.setError(getString(R.string.incorrect_password));

        if (!emailField && !passField) {
            String email = etMail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            pbLogin.setVisibility(View.VISIBLE);

            // Get user mail and password and sign in to the app
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            login();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Snackbar.make(view, Objects.requireNonNull(e.getMessage()), Snackbar.LENGTH_SHORT).show();
                    pbLogin.setVisibility(View.INVISIBLE);
                }
            });

        }
    }

    //Enter to the main ui
    private void login() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            saveUserToken(user.getUid());
            Intent login = new Intent(this, MainActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(login);
            finish();
        }
    }

    /*Sign in with user google account:
    open new google intent then the user choose one of his users form google account list
    and then back with google user information
     */
    private void googleSignIn(final View view) {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Returns from the google sign in form and enter to main activity via google account
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            pbLogin.setVisibility(View.VISIBLE);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                firebaseAuthWithGoogle(task.getResult(ApiException.class).getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                pbLogin.setVisibility(View.INVISIBLE);
                // ...
            }
        }
        if (requestCode == REGISTER_INFO) {
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra("email");
                String password = data.getStringExtra("password");
                etMail.setText(email);
                etPassword.setText(password);
            }
        }
    }

    // Enter to firebase and take google credential to enter google account
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            saveGoogleUserInfoAndLogin();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                            pbLogin.setVisibility(View.INVISIBLE);
                        }

                        // ...
                    }
                });
    }

    private void saveGoogleUserInfoAndLogin() {
        final GoogleSignInAccount googleUser = GoogleSignIn.getLastSignedInAccount(LoginActivity.this);
        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference().
                    child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        User user = new User();
                        if (googleUser != null && googleUser.getPhotoUrl() != null) {
                            user.setFirst(googleUser.getGivenName());
                            user.setLast(googleUser.getFamilyName());
                            user.setEmail(googleUser.getEmail());
                            user.setUid(uid);
                            String pid = UUID.randomUUID().toString();
                            user.setPid(pid);
                            snapshot.getRef().setValue(user);

                            //Upload google image to firebase storage
                            String photoURL = googleUser.getPhotoUrl().toString();
                            uploadGooglePhotoAndLogin(photoURL, uid, pid);
                        }
                    } else login();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    private void uploadGooglePhotoAndLogin(String photoUrl, final String uid, final String pid) {
        Glide.with(this).load(photoUrl).into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                //Upload Google picture and then login
                FirebaseStorage.getInstance().getReference().
                        child("images").child("users").child(pid).putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        login();
                    }
                });
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private void saveUserToken(final String uid) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                FirebaseDatabase.getInstance().getReference().
                        child("tokens").child(uid).setValue(s);
            }
        });
    }
}