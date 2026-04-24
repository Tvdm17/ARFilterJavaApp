package ai.deepar.deepar_example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
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

    // SQL — fetch makeovers owned by the logged-in client:
    // SELECT m.makeoverID, m.name, m.deeparFile
    // FROM makeover m
    // JOIN purchase p ON m.makeoverID = p.makeoverID
    // WHERE p.clientNumber = ?

    // SQL — load username for top bar:
    // SELECT fullName FROM client WHERE clientNumber = ?

    private static final int COLUMN_THRESHOLD = 5;

    private final List<Makeover> allMakeovers = new ArrayList<>();
    private final List<Makeover> displayedMakeovers = new ArrayList<>();
    private MakeoverAdapter adapter;
    private GridLayoutManager gridLayoutManager;

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

        loadDummyItems();
        setupRecyclerView();
        setupSearch();
        setupMenuButton();
    }

    private void loadDummyItems() {
        // Replace with DB query (see SQL comment at top) when DB is ready
        allMakeovers.add(new Makeover("Gray Blue Eyeshadow", "1GrayBlueEyeshadow.deepar"));
        allMakeovers.add(new Makeover("Purple Eyeliner",     "2PurpleEyeliner.deepar"));
        allMakeovers.add(new Makeover("Red Lips",            "3RedLips.deepar"));
        allMakeovers.add(new Makeover("Contour Lipgloss",    "4ContourLipgloss.deepar"));
        allMakeovers.add(new Makeover("Pale Blush",          "5MPaleSymmetricalBlush.deepar"));
        displayedMakeovers.addAll(allMakeovers);
    }

    private void setupRecyclerView() {
        RecyclerView rvItems = findViewById(R.id.rvItems);
        int columns = displayedMakeovers.size() > COLUMN_THRESHOLD ? 2 : 1;
        gridLayoutManager = new GridLayoutManager(this, columns);
        rvItems.setLayoutManager(gridLayoutManager);
        adapter = new MakeoverAdapter(this, displayedMakeovers);
        rvItems.setAdapter(adapter);
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
        });
    }

    private void setupMenuButton() {
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show());
    }

    private void filterList(String query) {
        displayedMakeovers.clear();
        for (Makeover m : allMakeovers) {
            if (m.makeoverName.toLowerCase().contains(query.toLowerCase())) {
                displayedMakeovers.add(m);
            }
        }
        int columns = displayedMakeovers.size() > COLUMN_THRESHOLD ? 2 : 1;
        gridLayoutManager.setSpanCount(columns);
        adapter.notifyDataSetChanged();
    }
}
