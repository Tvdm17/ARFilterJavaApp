package ai.deepar.deepar_example;

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

        int id = DatabaseManager.getUserid();

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

        DatabaseManager.fetchOwnedMakeovers(id, new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                try {

                    DatabaseManager.ownedMakeovers.clear(); // avoid duplicates

                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);

                        // get data from DB
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
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(CustomerHomeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });

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
