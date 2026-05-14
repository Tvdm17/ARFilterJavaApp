package tech.virtuglow.android;

import android.content.Intent;
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

public class CreatorHomeActivity extends DrawerMenu {

    private final List<Makeover> displayList = new ArrayList<>();
    private MakeoverAdapter adapter;
    private RecyclerView rvMasks;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_creator_home);

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        startDrawer();

        // ── RecyclerView ─────────────────────────────────────────────────────
        rvMasks = findViewById(R.id.rvMasks);
        rvMasks.setLayoutManager(new GridLayoutManager(this, 1));
        // reuse MakeoverAdapter; tapping navigates to CreatorItemCardActivity
        adapter = new MakeoverAdapter(this, displayList);
        adapter.setOnItemClickListener(makeoverId -> {
            Intent intent = new Intent(this, CreatorItemCardActivity.class);
            intent.putExtra("MAKEOVER_ID", makeoverId);
            startActivity(intent);
        });
        rvMasks.setAdapter(adapter);

        // ── Search ───────────────────────────────────────────────────────────
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

        // ── Add mask button → EditMaskActivity (create mode) ─────────────────
        findViewById(R.id.btnAddMask).setOnClickListener(v ->
                startActivity(new Intent(this, EditMaskActivity.class)));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseManager.fetchCreatorMakeovers(DatabaseManager.getUserid(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                DatabaseManager.creatorMakeovers.clear();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject obj = response.getJSONObject(i);
                        DatabaseManager.creatorMakeovers.add(new Makeover(
                                obj.optInt("makeoverID", 0),
                                obj.optString("name", "Unnamed"),
                                obj.optString("deeparFile", ""),
                                obj.optString("imagePreview", "default.jpg"),
                                obj.optDouble("price", 0.0),
                                obj.optDouble("averageRating", 0.0)
                        ));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                applySearch();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(CreatorHomeActivity.this, "Could not load your masks: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applySearch() {
        displayList.clear();
        for (Makeover m : DatabaseManager.creatorMakeovers) {
            if (currentQuery.isEmpty() || m.getName().toLowerCase().contains(currentQuery)) {
                displayList.add(m);
            }
        }
        int columns = displayList.size() > 8 ? 2 : 1;
        ((GridLayoutManager) rvMasks.getLayoutManager()).setSpanCount(columns);
        adapter.notifyDataSetChanged();
    }
}
