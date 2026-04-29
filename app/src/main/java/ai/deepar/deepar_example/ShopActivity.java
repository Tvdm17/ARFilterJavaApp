package ai.deepar.deepar_example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    public ShopAdapter myAdapter;
    public List<ShopItem> itemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        NavigationView navigationView = findViewById(R.id.navigationView);

        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_customer_home) {
                Intent intent = new Intent(this, CustomerHomeActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // DUMMY LIST FOR TESTING, TO BE REPLACED WITH DATABASE
        //List<Makeover> makeovers = new ArrayList<>();
        itemList.add(new ShopItem(3342,"Makeup Look 1", "" , "1GrayBlueEyeshadow.deepar", 4.99,5));
        itemList.add(new ShopItem(345,"Makeup Look 2", "" , "2PurpleEyeliner.deepar", 3.22,7.60));
        itemList.add(new ShopItem(678,"Makeup Look 3", "" , "3RedLips.deeparr", 1.5,1.25));
        itemList.add(new ShopItem(6745,"Makeup Look 4", "" , "4ContourLipgloss.deepar", 3,3.99));
        itemList.add(new ShopItem(3689,"Makeup Look 5", "" , "5MPaleSymmetricalBlush.deepar", 2.54,0.99));

        RecyclerView rvItems = findViewById(R.id.rvShopItems);
        int columns = itemList.size() > 5 ? 2 : 1;
        rvItems.setLayoutManager(new GridLayoutManager(this, columns));
        myAdapter = new ShopAdapter(this, itemList);
        rvItems.setAdapter(myAdapter);

        // CAN NOW ADAPT THE RECYCLEVIEW!!

        myAdapter.notifyDataSetChanged();
    }
}