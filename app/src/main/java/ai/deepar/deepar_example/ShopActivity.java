package ai.deepar.deepar_example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity implements FilterDialogFragment.OnFiltersApplied {

    // SQL — fetch all shop items with average rating:
    // SELECT m.makeoverID, m.name, m.imagePreview, m.deeparFile, m.price,
    //        COALESCE(AVG(r.score), 0) AS averageRating
    // FROM makeover m
    // LEFT JOIN review r ON m.makeoverID = r.makeoverID
    // GROUP BY m.makeoverID

    // SQL — fetch items filtered by selected categories:
    // SELECT DISTINCT m.makeoverID, m.name, m.imagePreview, m.deeparFile, m.price,
    //        COALESCE(AVG(r.score), 0) AS averageRating
    // FROM makeover m
    // LEFT JOIN review r ON m.makeoverID = r.makeoverID
    // JOIN makeovartag mt ON m.makeoverID = mt.makeoverID
    // JOIN tag t ON mt.tagName = t.tagName
    // WHERE t.`group` IN (?, ?, ...)
    // GROUP BY m.makeoverID

    // SQL — load username for top bar:
    // SELECT fullName FROM client WHERE clientNumber = ?

    private final List<ShopItem> allItems = new ArrayList<>();
    private final List<ShopItem> displayedItems = new ArrayList<>();
    private ShopAdapter shopAdapter;
    private List<String> activeCategories = new ArrayList<>();
    private String searchQuery = "";

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

        loadDummyItems();
        setupRecyclerView();
        setupSearch();
        setupFilterButton();
        setupMenuButton();
    }

    private void loadDummyItems() {
        // Replace with DB query (see SQL comment at top) when DB is ready
        allItems.add(new ShopItem(1, "Gray Blue Eyeshadow", null, "1GrayBlueEyeshadow.deepar", 4.5, 29.99));
        allItems.add(new ShopItem(2, "Purple Eyeliner",     null, "2PurpleEyeliner.deepar",    3.8, 19.99));
        allItems.add(new ShopItem(3, "Red Lips",            null, "3RedLips.deepar",            4.2, 24.99));
        allItems.add(new ShopItem(4, "Contour Lipgloss",    null, "4ContourLipgloss.deepar",    4.0, 34.99));
        allItems.add(new ShopItem(5, "Pale Blush",          null, "5MPaleSymmetricalBlush.deepar", 3.5, 22.99));
        displayedItems.addAll(allItems);
    }

    private void setupRecyclerView() {
        RecyclerView rvShopItems = findViewById(R.id.rvShopItems);
        rvShopItems.setLayoutManager(new LinearLayoutManager(this));
        shopAdapter = new ShopAdapter(this, displayedItems);
        rvShopItems.setAdapter(shopAdapter);
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }
        });
    }

    private void setupFilterButton() {
        ImageView ivFilter = findViewById(R.id.ivFilter);
        ivFilter.setOnClickListener(v -> {
            FilterDialogFragment dialog = new FilterDialogFragment();
            dialog.setOnFiltersAppliedListener(this);
            dialog.show(getSupportFragmentManager(), "FilterDialog");
        });
    }

    private void setupMenuButton() {
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onFiltersApplied(List<String> selectedCategories) {
        activeCategories = selectedCategories;
        // When DB is ready: re-fetch from DB with category WHERE clause (see SQL at top)
        // For now, re-apply in-memory search only
        applyFilters();
    }

    private void applyFilters() {
        displayedItems.clear();
        for (ShopItem item : allItems) {
            if (item.name.toLowerCase().contains(searchQuery.toLowerCase())) {
                displayedItems.add(item);
            }
        }
        shopAdapter.notifyDataSetChanged();

        TextView tvUsername = findViewById(R.id.tvUsername);
        // Replace with: tvUsername.setText(currentUser.getFullName()); once DB is wired
        tvUsername.setText("Shop");
    }
}
