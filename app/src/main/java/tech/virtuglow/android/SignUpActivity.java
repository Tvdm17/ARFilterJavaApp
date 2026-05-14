package tech.virtuglow.android;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUpActivity extends AppCompatActivity {

    private Button btnCustomer;
    private Button btnCreator;

    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbTerms;
    private boolean isCustomerSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        TextView tvSignIn = findViewById(R.id.tvSignIn);
        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        TextView tvTermsLink = findViewById(R.id.tvTermsLink);
        tvTermsLink.setOnClickListener(v -> {
            startActivity(new Intent(this, TermsActivity.class));
        });

        TextView tvPrivacyLink = findViewById(R.id.tvPrivacyLink);
        tvPrivacyLink.setOnClickListener(v -> {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
        });

        // intialise views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbTerms = findViewById(R.id.cbTerms);

        Button btnContinue = findViewById(R.id.btnContinue); // keep

        btnContinue.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this,"Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!cbTerms.isChecked()){
                Toast.makeText(this, "Agree to the terms of service to continue", Toast.LENGTH_SHORT).show();
                return;
            }
            String hashedPassword = DatabaseManager.hashPassword(password);
            btnContinue.setEnabled(false);

            DatabaseManager.postToAPI("create_user", new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    String roleEndpoint = "";
                    if(isCustomerSelected){
                        roleEndpoint = "create_client";
                    }
                    else{
                        roleEndpoint = "create_mua";
                    }
                    // you need to do DatabaseManager.SimpleCallback, DatebaseManager is a class, but we need the interface
                    //  contains the onSucces and onFailure methods, which are overwritten to do what you want to do
                    DatabaseManager.postToAPI(roleEndpoint, new DatabaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_LONG).show();
                            // go to login page
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            finish();
                        }

                        @Override
                        public void onFailure(String msg) {
                            btnContinue.setEnabled(true);
                            Toast.makeText(SignUpActivity.this, "Error linking role in signup: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    }, email);
                }

                @Override
                public void onFailure(String msg) {
                    btnContinue.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, "User creation failed (Email might be taken)", Toast.LENGTH_LONG).show();
                }
            }, email, hashedPassword);






        });

        btnCustomer = findViewById(R.id.btnCustomer);
        btnCreator  = findViewById(R.id.btnCreator);

        btnCustomer.setOnClickListener(v -> setCustomerSelected(true));
        btnCreator .setOnClickListener(v -> setCustomerSelected(false));

        // Default state: Customer selected
        setCustomerSelected(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setCustomerSelected(boolean customerActive) {
        this.isCustomerSelected = customerActive;
        applyButtonState(btnCustomer, customerActive);
        applyButtonState(btnCreator, !customerActive);
    }

    private void applyButtonState(Button btn, boolean isActive) {
        btn.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, isActive ? R.color.buttonPrimary : R.color.buttonDisabled)
        ));
        btn.setTextColor(
                ContextCompat.getColor(this, isActive ? R.color.textOnGold : R.color.textSecondary)
        );
    }
}