package ai.deepar.deepar_example;
import android.os.Bundle;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ShopActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // DUMMY LIST FOR TESTING, TO BE REPLACED WITH DATABASE
        List<ShopMakeover> makeovers = new ArrayList<>();
        makeovers.add(new ShopMakeover("Makeup Look 1", R.drawable.logo, 4.99, 4.5f));
        makeovers.add(new ShopMakeover("Makeup Look 2", R.drawable.logo, 2.00, 5.0f));
        makeovers.add(new ShopMakeover("Makeup Look 3", R.drawable.logo, 3.50, 3.6f));
        makeovers.add(new ShopMakeover("Makeup Look 4", R.drawable.logo, 4.20, 4.9f));
        makeovers.add(new ShopMakeover("Makeup Look 5", R.drawable.logo, 6.20, 2.3f));
        RecyclerView rvShopItems = findViewById(R.id.rvShopItems);
        int columns = makeovers.size() > 5 ? 2 : 1;
        rvShopItems.setLayoutManager(new GridLayoutManager(this, columns));
        rvShopItems.setAdapter(new ShopMakeoverAdapter(this, makeovers));
    }
}