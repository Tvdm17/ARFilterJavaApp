package tech.virtuglow.android;


import static tech.virtuglow.android.DatabaseManager.isIsCustomer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        View header = navigationView.getHeaderView(0);


        TextView tvNavUsername = header.findViewById(R.id.tvNavUsername);
        tvNavUsername.setText(DatabaseManager.getUsername());

        TextView tvNavEmail = header.findViewById(R.id.tvNavEmail);
        tvNavEmail.setText(DatabaseManager.getEmail());


        TextView tvChanging = header.findViewById(R.id.tvChanging);
        TextView tvHome = header.findViewById(R.id.tvHome);
        TextView tvProfile = header.findViewById(R.id.tvProfile);
        ImageView ivChanging = header.findViewById(R.id.ivChanging);

        if (!isIsCustomer()) {
            tvHome.setOnClickListener(v -> {
                startActivity(new Intent(this, CreatorHomeActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UserPageActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvChanging.setText(R.string.nav_reviews);
            ivChanging.setImageResource(R.drawable.reviews_icon);
            tvChanging.setOnClickListener(v -> {
                startActivity(new Intent(this, CreatorReportsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

        } else {
            tvHome.setOnClickListener(v -> {
                startActivity(new Intent(this, CustomerHomeActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UserPageActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            tvChanging.setText(R.string.nav_shop);
            ivChanging.setImageResource(R.drawable.shop_icon);
            tvChanging.setOnClickListener(v -> {
                startActivity(new Intent(this, ShopActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        TextView tvSignOut = header.findViewById(R.id.tvSignOut);
        tvSignOut.setOnClickListener(v -> {
            DatabaseManager.setUserid(-1);
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE); // wipe disk
            prefs.edit().clear().apply();
            DatabaseManager.setUsername("");
            DatabaseManager.setEmail("");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

    }
}
