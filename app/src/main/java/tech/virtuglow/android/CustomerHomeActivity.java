package tech.virtuglow.android;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
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

public class CustomerHomeActivity extends DrawerMenu {

    private MakeoverAdapter myAdapter;
    private final List<Makeover> displayList = new ArrayList<>();
    private String currentQuery = "";
    private RecyclerView rvItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        startDrawer();

        // recover if app is inactive and gets reset
        if(DatabaseManager.getUserid() == -1){
            int recover = getIntent().getIntExtra("USER_ID", -1);
            DatabaseManager.setUserid(recover);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvItems = findViewById(R.id.rvItems);
        rvItems.setLayoutManager(new GridLayoutManager(this, 1));
        myAdapter = new MakeoverAdapter(this, displayList);
        rvItems.setAdapter(myAdapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                applySearch();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUIFromGlobalList();
        DatabaseManager.fetchOwnedMakeovers(DatabaseManager.getUserid(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                try {
                    DatabaseManager.ownedMakeovers.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        DatabaseManager.ownedMakeovers.add(new Makeover(
                                obj.optInt("makeoverID", 0),
                                obj.getString("name"),
                                obj.getString("deeparFile"),
                                obj.optString("imagePreview", "default.jpg"),
                                obj.optDouble("price", 0.0),
                                obj.optDouble("averageRating", 0.0)
                        ));
                    }
                    applySearch();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                refreshUIFromGlobalList(); // safety refresh
            }



            @Override
            public void onFailure(String message) {
                Toast.makeText(CustomerHomeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshUIFromGlobalList() {
        displayList.clear();
        displayList.addAll(DatabaseManager.ownedMakeovers);

        // Re-apply search/filtering if a query exists, this is for when you where searching but didnt exit the search bar

        if (!currentQuery.isEmpty()) {
            String query = currentQuery.toLowerCase();
            java.util.Iterator<Makeover> iterator = displayList.iterator();

            while (iterator.hasNext()) {
                Makeover m = iterator.next();
                // remove if not in search query
                if (!m.getName().toLowerCase().contains(query)) {
                    iterator.remove();
                }
            }
        }


        int columns = (displayList.size() > 8) ? 2 : 1;
        ((GridLayoutManager) rvItems.getLayoutManager()).setSpanCount(columns);
        myAdapter.notifyDataSetChanged();
    }

    private void applySearch() {
        displayList.clear();
        for (Makeover m : DatabaseManager.ownedMakeovers) {
            if (currentQuery.isEmpty() || m.getName().toLowerCase().contains(currentQuery)) {
                displayList.add(m);
            }
        }
        // number of columns based on the amount of items
        int columns = (displayList.size() > 8) ? 2 : 1;
        ((GridLayoutManager) rvItems.getLayoutManager()).setSpanCount(columns);
        myAdapter.notifyDataSetChanged();
    }
}
