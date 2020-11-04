package arikz.easyride.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import arikz.easyride.R;
import arikz.easyride.objects.User;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = ".RegisterActivity";
    private TextInputEditText etFirst, etLast, etMail, etPhone, etPassword;
    private ProgressBar pbRegister;

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
        MaterialButton btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean emptyField = Objects.requireNonNull(etFirst.getText()).toString().isEmpty() || Objects.requireNonNull(etLast.getText()).toString().isEmpty() ||
                        Objects.requireNonNull(etMail.getText()).toString().isEmpty() || Objects.requireNonNull(etPassword.getText()).toString().isEmpty() ||
                        Objects.requireNonNull(etPhone.getText()).toString().isEmpty();
                //TODO CHECK PHONE NUMBER IS CORRECT
                if (emptyField) {
                    Toast.makeText(RegisterActivity.this, R.string.enter_fields, Toast.LENGTH_SHORT).show();
                } else {
                    //Show progress bar
                    pbRegister.setVisibility(View.VISIBLE);

                    //Collect email and password details
                    String email = etMail.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    Intent data = new Intent();
                    data.putExtra("email",email);
                    data.putExtra("password",password);
                    setResult(RESULT_OK,data);

                    // Register new user using firebase authorization
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Creating new user from the details the user entered in the text fields
                                User user = new User();
                                user.setEmail(etMail.getText().toString().trim());
                                user.setFirst(etFirst.getText().toString().trim());
                                user.setLast(etLast.getText().toString().trim());
                                user.setPhone(etPhone.getText().toString().trim());
                                user.setUid(Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getUser()).getUid());

                                // New access into firebase to store user information
                                FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).setValue(user);

                                //Sign out from user
                                FirebaseAuth.getInstance().signOut();
                                Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                                finish();
                            }
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
        });
    }
}