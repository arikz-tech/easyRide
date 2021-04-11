package arik.easyride.ui.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arik.easyride.R;
import arik.easyride.models.User;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = ".RegisterActivity";
    private TextInputEditText etFirst, etLast, etMail, etPhone, etPassword;
    private ProgressBar pbRegister;
    private AutoCompleteTextView etArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Attach layout component
        etFirst = findViewById(R.id.etFirst);
        etLast = findViewById(R.id.etLast);
        etMail = findViewById(R.id.etMail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        pbRegister = findViewById(R.id.pbRegister);
        etArea = findViewById(R.id.etArea);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);

        String[] areas = {"050", "051", "052", "053","054"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, areas);
        etArea.setAdapter(adapter);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerNewUser();
            }
        });
    }

    private void registerNewUser() {
        boolean emptyField = etFirst.getText().toString().isEmpty() || etLast.getText().toString().isEmpty() ||
                etMail.getText().toString().isEmpty() || etPassword.getText().toString().isEmpty() ||
                etPhone.getText().toString().isEmpty() || etArea.getText().toString().isEmpty();
        if (emptyField) {
            Toast.makeText(RegisterActivity.this, R.string.enter_fields, Toast.LENGTH_SHORT).show();
        } else {
            //Show progress bar
            pbRegister.setVisibility(View.VISIBLE);

            //Collect email and password details
            String email = etMail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            Intent data = new Intent();
            data.putExtra("email", email);
            data.putExtra("password", password);
            setResult(RESULT_OK, data);

            // Register new user using firebase authorization
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    // Creating new user from the details the user entered in the text fields
                    User user = new User();
                    user.setEmail(etMail.getText().toString().trim());
                    user.setFirst(etFirst.getText().toString().trim());
                    user.setLast(etLast.getText().toString().trim());
                    user.setPhone(etArea.getText().toString().trim() + etPhone.getText().toString().trim());
                    user.setPid("avatar_logo.png");
                    user.setUid(Objects.requireNonNull(authResult.getUser()).getUid());

                    // New access into firebase to store user information
                    FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Sign out from user
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pbRegister.setVisibility(View.INVISIBLE);
                    Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}