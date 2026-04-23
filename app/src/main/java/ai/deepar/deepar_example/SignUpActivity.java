package ai.deepar.deepar_example;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUpActivity extends AppCompatActivity {

    private Button btnCustomer;
    private Button btnCreator;

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

        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerHomeActivity.class));
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