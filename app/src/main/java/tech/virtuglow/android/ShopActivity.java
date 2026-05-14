package tech.virtuglow.android;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
    private List<ShopItem> displayList = new ArrayList<>();

    private String currentQuery = "";
    private List<String> activeFilters = new ArrayList<>();
    private List<String> availableTags = new ArrayList<>();
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
            dialog.setAvailableTags(availableTags);
            dialog.setPreSelectedTags(activeFilters);
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

        if(selectedCategories.isEmpty()){
            fetchItems(); // refresh shop
            return;
        }

        String tagsParam = String.join(",", selectedCategories);
        DatabaseManager.fetchShopItemsFiltered(DatabaseManager.getUserid(), tagsParam, new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                updateDisplayList(response);
            }

            @Override
            public void onFailure(String message) {
                displayList.clear();
                myAdapter.notifyDataSetChanged();
                Toast.makeText(ShopActivity.this, "No matching items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDisplayList(JSONArray response) {
        displayList.clear();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.getJSONObject(i);
                ShopItem item = new ShopItem(
                        obj.getInt("makeoverID"),
                        obj.getString("name"),
                        obj.getString("deeparFile"),
                        obj.getString("imagePreview"),
                        obj.optDouble("price", 0.0),
                        parseRawRating(obj)
                );
                displayList.add(item);
            }
            myAdapter.notifyDataSetChanged();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseManager.fetchAllTags(new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                availableTags.clear();
                for (int i = 0; i < response.length(); i++) {
                    JSONObject obj = response.optJSONObject(i);
                    if (obj != null) {
                        String tag = obj.optString("tagName", obj.optString("group", ""));
                        if (!tag.isEmpty() && !availableTags.contains(tag)) availableTags.add(tag);
                    }
                }
            }
            @Override
            public void onFailure(String message) {}
        });
        fetchItems();
    }

    private void fetchItems(){
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
                                parseRawRating(obj)
                        );
                        Log.d("JSON_DATA", "API returned: " + obj.optDouble("averageRating"));
                        itemList.add(newItem);
                        DatabaseManager.shopItems.add(newItem);
                    }
                    applyFilters();
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

    private double parseRawRating(JSONObject obj) {

        String rawrating = obj.optString("averageRating", "0.0");
        double rating;
        try{
            rating = Double.parseDouble(rawrating);
            if(Double.isNaN(rating)){rating = 0.0;}
        }
        catch (NullPointerException | NumberFormatException e){
            rating = 0.0;
        }
        return rating;
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
        if (!item.isTagsLoaded()) return false;
        for (String filter : activeFilters) {
            for (String tag : item.getTags()) {
                if (tag.equalsIgnoreCase(filter)) return true;
            }
        }
        return false;
    }
}
