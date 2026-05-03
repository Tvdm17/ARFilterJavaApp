package ai.deepar.deepar_example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONObject;

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

        String username = DatabaseManager.getUsername();

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(username);



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

        ;

        RecyclerView rvItems = findViewById(R.id.rvShopItems);
        int columns = itemList.size() > 8 ? 2 : 1;
        rvItems.setLayoutManager(new GridLayoutManager(this, columns));
        myAdapter = new ShopAdapter(this, itemList);
        rvItems.setAdapter(myAdapter);


        int id = DatabaseManager.getUserid();

        DatabaseManager.fetchShopItems(id, new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                itemList.clear();
                DatabaseManager.shopItems.clear(); // shopItems !!!!!!!!!
                if(response.length() == 0){
                    Toast.makeText(ShopActivity.this, "You own all makeovers, come back later.", Toast.LENGTH_LONG).show();
                    // message to show if there you own all makeovers
                }
                try {
                    for(int i = 0; i < response.length(); i++){

                        JSONObject obj = response.getJSONObject(i);

                        ShopItem newItem = new ShopItem(
                           obj.getInt("makeoverID"),
                           obj.getString("name"),
                           obj.getString("deeparFile"),
                           obj.getString("imagePreview"),
                           obj.optDouble("price", 0.0),
                           obj.optDouble("averageRating",0.0)

                        );

                        itemList.add(newItem);
                        DatabaseManager.shopItems.add(newItem);

                    }

                    int columns = itemList.size() > 8 ? 2 : 1;
                    ((GridLayoutManager) rvItems.getLayoutManager()).setSpanCount(columns);
                    myAdapter.notifyDataSetChanged();

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(ShopActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });



        // CAN NOW ADAPT THE RECYCLEVIEW!!

    }
}