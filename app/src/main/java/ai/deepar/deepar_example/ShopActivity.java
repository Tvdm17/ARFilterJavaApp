package ai.deepar.deepar_example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends DrawerMenu implements FilterDialogFragment.OnFiltersApplied {

    private ShopAdapter myAdapter;
    private final List<ShopItem> itemList = new ArrayList<>();
    private final List<ShopItem> displayList = new ArrayList<>();

    private String currentQuery = "";
    private List<String> activeFilters = new ArrayList<>();
    private RecyclerView rvItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        startDrawer();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvItems = findViewById(R.id.rvShopItems);
        rvItems.setLayoutManager(new GridLayoutManager(this, 1));
        myAdapter = new ShopAdapter(this, displayList);
        rvItems.setAdapter(myAdapter);

        ImageView ivFilter = findViewById(R.id.ivFilter);
        ivFilter.setOnClickListener(v -> {
            FilterDialogFragment dialog = new FilterDialogFragment();
            dialog.setOnFiltersAppliedListener(this);
            dialog.show(getSupportFragmentManager(), "FilterDialog");
        });

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }
        });

    }

    @Override
    public void onFiltersApplied(List<String> selectedCategories) {
        activeFilters = selectedCategories;
        applyFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseManager.fetchShopItems(DatabaseManager.getUserid(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                itemList.clear();
                DatabaseManager.shopItems.clear();
                if (response.length() == 0) {
                    Toast.makeText(ShopActivity.this, "You own all makeovers, come back later.", Toast.LENGTH_LONG).show();
                }
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        ShopItem newItem = new ShopItem(
                                obj.getInt("makeoverID"),
                                obj.getString("name"),
                                obj.getString("deeparFile"),
                                obj.getString("imagePreview"),
                                obj.optDouble("price", 0.0),
                                obj.optDouble("averageRating", 0.0)
                        );
                        itemList.add(newItem);
                        DatabaseManager.shopItems.add(newItem);
                    }
                    applyFilters();
                    fetchTagsForAll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(ShopActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        displayList.clear();
        for (ShopItem item : DatabaseManager.shopItems) {
            if (matchesSearch(item) && matchesTagFilter(item)) {
                displayList.add(item);
            }
        }
        int columns = displayList.size() > 8 ? 2 : 1;
        ((GridLayoutManager) rvItems.getLayoutManager()).setSpanCount(columns);
        myAdapter.notifyDataSetChanged();
    }

    private boolean matchesSearch(ShopItem item) {
        return currentQuery.isEmpty() || item.getName().toLowerCase().contains(currentQuery);
    }

    private boolean matchesTagFilter(ShopItem item) {
        if (activeFilters.isEmpty()) return true;
        if (!item.isTagsLoaded()) return true;
        for (String filter : activeFilters) {
            if (item.getTags().contains(filter)) return true;
        }
        return false;
    }

    private void fetchTagsForAll() {
        if (itemList.isEmpty()) return;
        int[] remaining = {itemList.size()};
        for (ShopItem item : itemList) {
            DatabaseManager.fetchTagsForMakeover(item.getId(), new DatabaseManager.APICallback() {
                @Override
                public void onSuccess(JSONArray response) {
                    List<String> tags = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        if (obj != null) {
                            String group = obj.optString("group", "");
                            if (!group.isEmpty()) tags.add(group);
                        }
                    }
                    item.setTags(tags);
                    remaining[0]--;
                    if (remaining[0] == 0) applyFilters();
                }

                @Override
                public void onFailure(String message) {
                    item.setTags(new ArrayList<>());
                    remaining[0]--;
                    if (remaining[0] == 0) applyFilters();
                }
            });
        }
    }
}
