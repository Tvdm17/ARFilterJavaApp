package tech.virtuglow.android;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UserPageActivity extends DrawerMenu {

    private EditText etChangeUsername, etChangeEmail, etChangePassword;
    private Button btnChangeUsername, btnChangeEmail, btnChangePassword;
    private TextView tvUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_page);

        startDrawer();

        etChangeUsername = findViewById(R.id.etChangeUsername);
        etChangeEmail = findViewById(R.id.etChangeEmail);
        etChangePassword = findViewById(R.id.etChangePassword);

        tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setText(DatabaseManager.getEmail());


        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangeUsername.setOnClickListener(v -> {
            String username = etChangeUsername.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Username will not be changed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // disable button to prevent multiple logins
            btnChangeUsername.setEnabled(false);

            DatabaseManager.postToAPI("update_username", new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    DatabaseManager.setUsername(username);
                    btnChangeUsername.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Username changed successfully!", Toast.LENGTH_SHORT).show();
                    etChangeUsername.setText("");
                    tvUsername.setText(username);
                    android.view.View header = navigationView.getHeaderView(0);
                    ((TextView) header.findViewById(R.id.tvNavUsername)).setText(username);
                }

                @Override
                public void onFailure(String message) {
                    btnChangeUsername.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Username change failed: " + message, Toast.LENGTH_SHORT).show();
                }
            }, username, String.valueOf(DatabaseManager.getUserid()));
        });


        btnChangeEmail.setOnClickListener(v -> {
            String email = etChangeEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Email will not be changed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // disable button to prevent multiple logins
            btnChangeEmail.setEnabled(false);

            DatabaseManager.postToAPI("update_email", new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    btnChangeEmail.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Email changed successfully!", Toast.LENGTH_SHORT).show();
                    etChangeEmail.setText("");
                }

                @Override
                public void onFailure(String message) {
                    btnChangeEmail.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Email change failed: " + message, Toast.LENGTH_SHORT).show();
                }
            }, email, String.valueOf(DatabaseManager.getUserid()));
        });

        btnChangePassword.setOnClickListener(v -> {

            String password = etChangePassword.getText().toString().trim();

            if (password.isEmpty()) {
                Toast.makeText(this, "Password will not be changed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // disable button to prevent multiple logins
            btnChangePassword.setEnabled(false);

            String hashedPassword = DatabaseManager.hashPassword(password);

            DatabaseManager.postToAPI("update_password", new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                    etChangePassword.setText("");
                }

                @Override
                public void onFailure(String message) {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Password change failed:" + message, Toast.LENGTH_SHORT).show();

                }
            }, hashedPassword, String.valueOf(DatabaseManager.getUserid()));
        });

        ImageView ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        ivPasswordToggle.setOnClickListener(v -> {
            if (etChangePassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etChangePassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etChangePassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
