package ai.deepar.deepar_example;

import android.os.Bundle;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
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

public class CustomerHomeActivity extends AppCompatActivity {
    public MakeoverAdapter myAdapter;
    public List<Makeover> makeoverList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);

        // recover if app is inactive and gets reset
        if(DatabaseManager.getUsername() == -1){
            int recover = getIntent().getIntExtra("USER_ID", -1);
            DatabaseManager.setUserid(recover);
        }


        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        NavigationView navigationView = findViewById(R.id.navigationView);

        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_shop) {
                Intent intent = new Intent(this, ShopActivity.class);
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

        RecyclerView rvItems = findViewById(R.id.rvItems);
        int columns = makeoverList.size() > 5 ? 2 : 1;
        rvItems.setLayoutManager(new GridLayoutManager(this, columns));
        myAdapter = new MakeoverAdapter(this, makeoverList);
        rvItems.setAdapter(myAdapter);

        // CAN NOW ADAPT THE RECYCLEVIEW!!

        myAdapter.notifyDataSetChanged();

        int id = DatabaseManager.getUsername();

        DatabaseManager.fetchOwnedMakeovers(id, new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                try {

                    makeoverList.clear(); // avoid duplicates

                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);

                        // get data from DB
                        makeoverList.add(new Makeover(
                                obj.optInt("makeoverID", 0),
                                obj.getString("name"),
                                obj.getString("deeparFile"),
                                obj.optString("imagePreview", "default.jpg"),
                                obj.optDouble("price", 0.0)
                        ));
                    }

                    // number of colums based on the amount of filters, change to what we want, optional though
                    int columns = (makeoverList.size() > 8) ? 2 : 1;
                    if(rvItems.getLayoutManager() instanceof  GridLayoutManager) {
                        ((GridLayoutManager) rvItems.getLayoutManager()).setSpanCount(columns);
                    }

                    // Refresh the ui
                    myAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(CustomerHomeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });

    }
}