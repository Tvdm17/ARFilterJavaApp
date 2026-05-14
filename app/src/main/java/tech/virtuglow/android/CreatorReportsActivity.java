package tech.virtuglow.android;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class CreatorReportsActivity extends DrawerMenu {

    private final List<Review> reviewList = new ArrayList<>();
    private CreatorReviewsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_creator_reviews);
        startDrawer();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvCreatorReviews);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CreatorReviewsAdapter(reviewList);
        rv.setAdapter(adapter);

        loadReviews();
    }

    private void loadReviews() {
        DatabaseManager.fetchCreatorReviews(DatabaseManager.getUserid(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                reviewList.clear();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        reviewList.add(Review.fromJson(response.getJSONObject(i)));
                    } catch (JSONException e) {
                        // skip malformed entry
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(CreatorReportsActivity.this, "Could not load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
