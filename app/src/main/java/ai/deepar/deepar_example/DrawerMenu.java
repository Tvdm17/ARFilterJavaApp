package ai.deepar.deepar_example;


import static ai.deepar.deepar_example.DatabaseManager.isIsCustomer;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class  DrawerMenu extends AppCompatActivity  {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;

    protected void startDrawer(){
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        View header = navigationView.getHeaderView(0);
        TextView tvNavUsername = header.findViewById(R.id.tvNavUsername);
        tvNavUsername.setText(DatabaseManager.getUsername());

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        TextView tvChanging = header.findViewById(R.id.tvChanging);

        TextView tvHome = header.findViewById(R.id.tvChanging);
        TextView tvProfile = header.findViewById(R.id.tvProfile);
        if (!isIsCustomer()) {
            tvHome.setOnClickListener(v -> {
                startActivity(new Intent(this, CreatorHomeActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvProfile.setOnClickListener(v-> {
                startActivity(new Intent(this, ClientProfile.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvChanging.setText("Reports");
            tvChanging.setOnClickListener(v -> {
                startActivity(new Intent(this, CreatorReports.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

        } else {
            tvHome.setOnClickListener(v -> {
                startActivity(new Intent(this, ShopActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvProfile.setOnClickListener(v-> {
                startActivity(new Intent(this, CreatorProfile.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvChanging.setText("Home");
            tvChanging.setOnClickListener(v -> {
                startActivity(new Intent(this, CustomerHomeActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        TextView tvSignOut = header.findViewById(R.id.tvSignOut);
        tvSignOut.setOnClickListener(v -> {
            DatabaseManager.setUserid(-1);
            DatabaseManager.setUsername("");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

    }
}
