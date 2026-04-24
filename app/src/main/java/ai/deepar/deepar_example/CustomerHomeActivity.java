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

public class CustomerHomeActivity extends AppCompatActivity {
    public MakeoverAdapter myAdapter;
    public List<Makeover> makeovers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // DUMMY LIST FOR TESTING, TO BE REPLACED WITH DATABASE
        //List<Makeover> makeovers = new ArrayList<>();
        makeovers.add(new Makeover("Makeup Look 1", "1GrayBlueEyeshadow.deepar"));
        makeovers.add(new Makeover("Makeup Look 2", "2PurpleEyeliner.deepar"));
        makeovers.add(new Makeover("Makeup Look 3", "3RedLips.deepar"));
        makeovers.add(new Makeover("Makeup Look 4", "4ContourLipgloss.deepar"));
        makeovers.add(new Makeover("Makeup Look 5", "5MPaleSymmetricalBlush.deepar"));

        RecyclerView rvItems = findViewById(R.id.rvItems);
        int columns = makeovers.size() > 5 ? 2 : 1;
        rvItems.setLayoutManager(new GridLayoutManager(this, columns));
        rvItems.setAdapter(new MakeoverAdapter(this, makeovers));

        myAdapter.notifyDataSetChanged();
    }



}