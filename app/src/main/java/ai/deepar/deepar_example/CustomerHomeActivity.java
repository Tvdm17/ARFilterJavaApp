package ai.deepar.deepar_example;

import android.os.Bundle;
import android.content.Intent;
import android.widget.ImageButton;

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

public class CustomerHomeActivity extends AppCompatActivity {
    public MakeoverAdapter myAdapter;
    public List<CustomerMakeover> makeoverList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);

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

        // DUMMY LIST FOR TESTING, TO BE REPLACED WITH DATABASE
        //List<Makeover> makeovers = new ArrayList<>();
        makeoverList.add(new CustomerMakeover("Makeup Look 1", "1GrayBlueEyeshadow.deepar"));
        makeoverList.add(new CustomerMakeover("Makeup Look 2", "2PurpleEyeliner.deepar"));
        makeoverList.add(new CustomerMakeover("Makeup Look 3", "3RedLips.deepar"));
        makeoverList.add(new CustomerMakeover("Makeup Look 4", "4ContourLipgloss.deepar"));
        makeoverList.add(new CustomerMakeover("Makeup Look 5", "5MPaleSymmetricalBlush.deepar"));

        RecyclerView rvItems = findViewById(R.id.rvItems);
        int columns = makeoverList.size() > 5 ? 2 : 1;
        rvItems.setLayoutManager(new GridLayoutManager(this, columns));
        myAdapter = new MakeoverAdapter(this, makeoverList);
        rvItems.setAdapter(myAdapter);

        // CAN NOW ADAPT THE RECYCLEVIEW!!

        myAdapter.notifyDataSetChanged();
    }
}