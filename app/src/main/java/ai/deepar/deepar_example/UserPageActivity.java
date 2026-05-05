package ai.deepar.deepar_example;

import android.content.Intent;
import android.media.MediaRouter;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_page);

        startDrawer();

        etChangeUsername = findViewById(R.id.etChangeUsername);
        etChangeEmail = findViewById(R.id.etChangeEmail);
        etChangePassword = findViewById(R.id.etChangePassword);

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setText(DatabaseManager.getUsername());


        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);;
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangeUsername.setOnClickListener(v -> {
            String username = etChangeUsername.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Username will not be changed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // disable button to prevent multiple logins
            btnChangeUsername.setEnabled(false);

            DatabaseManager.postToAPI("update_usernameENDPOINT", new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Username changed successfully!", Toast.LENGTH_SHORT).show();
                    etChangePassword.setText("");
                }

                @Override
                public void onFailure(String message) {
                    btnChangeUsername.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Username change failed:" + message, Toast.LENGTH_SHORT).show();
                }
            }, String.valueOf(DatabaseManager.getUserid()),username);
        });


        btnChangeEmail.setOnClickListener(v -> {
            String email = etChangeEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Email will not be changed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // disable button to prevent multiple logins
            btnChangeEmail.setEnabled(false);

            DatabaseManager.postToAPI("ENDPOINTNAMEFOREMAIL", new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                    etChangePassword.setText("");
                }

                @Override
                public void onFailure(String message) {
                    btnChangeEmail.setEnabled(true);
                    Toast.makeText(UserPageActivity.this, "Password change failed:" + message, Toast.LENGTH_SHORT).show();

                }
            }, String.valueOf(DatabaseManager.getUserid()),email);
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

            DatabaseManager.postToAPI("ENDPOINTNAMEFORPASSWORD", new DatabaseManager.SimpleCallback() {
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
            }, String.valueOf(DatabaseManager.getUserid()) ,hashedPassword);
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
