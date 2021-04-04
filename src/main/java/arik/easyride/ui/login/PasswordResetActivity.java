package arik.easyride.ui.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import arik.easyride.R;

public class PasswordResetActivity extends AppCompatActivity {
    private static final String TAG = ".PasswordResetActivity";
    private ProgressBar pbReset;
    private TextInputEditText etMail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivty_password_reset);

        //Attach layout component
        pbReset = findViewById(R.id.pbReset);
        etMail = findViewById(R.id.etMail);

        MaterialButton btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Objects.requireNonNull(etMail.getText()).toString().isEmpty()) {
                    Toast.makeText(PasswordResetActivity.this, R.string.please_enter_mail, Toast.LENGTH_SHORT).show();
                } else {
                    pbReset.setVisibility(View.VISIBLE);
                    //Getting authorisation from firebase and send email request using google services
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    String emailAddress = etMail.getText().toString();
                    auth.sendPasswordResetEmail(emailAddress)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(PasswordResetActivity.this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pbReset.setVisibility(View.INVISIBLE);
                            Toast.makeText(PasswordResetActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}