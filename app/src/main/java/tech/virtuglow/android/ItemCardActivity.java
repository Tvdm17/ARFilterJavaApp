package tech.virtuglow.android;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class ItemCardActivity extends DrawerMenu implements LeaveReviewDialogFragment.OnReviewSubmitted {

    public Makeover currentItem;
    private List<Review> reviewList;
    private ReviewAdapter reviewAdapter;

    private String currentMainFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_card);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        startDrawer();

        int id = getIntent().getIntExtra("MAKEOVER_ID", -1);
        currentItem = DatabaseManager.getMakeoverById(id);

        if (currentItem == null) {
            Toast.makeText(this, "Data sync error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentMainFileName = currentItem.getPreviewImage();

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        TextView tvMaskName = findViewById(R.id.tvMaskName);
        tvMaskName.setText(currentItem.getName());

        ImageView ivMainImage = findViewById(R.id.ivMainImage);

        ImageView[] thumbs = {
                findViewById(R.id.ivThumb1),
                findViewById(R.id.ivThumb2),
                findViewById(R.id.ivThumb3),
                findViewById(R.id.ivThumb4)
        };

        Glide.with(this).load(DatabaseManager.PREVIEW_URL + currentItem.getPreviewImage()).into(ivMainImage);

        // load secondary images
        DatabaseManager.fetchSecondaryImages(currentItem.getId(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                try {
                    for (int i = 0; i < response.length() && i < thumbs.length; i++) {
                        String fileName = response.getJSONObject(i).getString("fileName");
                        String fullUrl = DatabaseManager.PREVIEW_URL + fileName;

                        Glide.with(ItemCardActivity.this)
                                .load(fullUrl)
                                .centerCrop()
                                .into(thumbs[i]);
                        setupThumbClick(thumbs[i], fileName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String message) {
                // If it fails, the thumbs just stay empty/default
            }
        });

        // ── Add / Remove button ──────────────────────────────────────────────
        Button btnAddDelete   = findViewById(R.id.btnAddDelete);
        Button btnLeaveReview = findViewById(R.id.btnLeaveReview);
        boolean owned = DatabaseManager.isItemOwned(id);

        if (owned) {
            btnAddDelete.setText("Remove");
            btnAddDelete.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.error)));
            btnLeaveReview.setVisibility(View.VISIBLE);
            btnAddDelete.setOnClickListener(v -> {
                btnAddDelete.setEnabled(false);
                DatabaseManager.removePurchase(DatabaseManager.getUserid(), id, new DatabaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        DatabaseManager.ownedMakeovers.remove(currentItem);
                        DatabaseManager.shopItems.add((ShopItem) currentItem);
                        Toast.makeText(ItemCardActivity.this, "Removed from your collection", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        btnAddDelete.setEnabled(true);
                        Toast.makeText(ItemCardActivity.this, "Remove failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            btnAddDelete.setText("Add (" + currentItem.getPrice() + ")");
            btnAddDelete.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.buttonPrimary)));
            btnLeaveReview.setVisibility(View.GONE);
            btnAddDelete.setOnClickListener(v -> {
                btnAddDelete.setEnabled(false);
                DatabaseManager.addPurchase(DatabaseManager.getUserid(), id, new DatabaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        DatabaseManager.ownedMakeovers.add(currentItem);
                        DatabaseManager.shopItems.remove(currentItem);
                        Toast.makeText(ItemCardActivity.this, "Added to your collection!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        btnAddDelete.setEnabled(true);
                        Toast.makeText(ItemCardActivity.this, "Purchase failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        // ── Rating display ───────────────────────────────────────────────────
        RatingBar rbAverageRating = findViewById(R.id.rbAverageRating);
        rbAverageRating.setRating((float) currentItem.getAverageRating());

        TextView tvAverageValue = findViewById(R.id.tvAverageValue);
        tvAverageValue.setText(String.format(Locale.getDefault(), "%.1f / 5", currentItem.getAverageRating()));

        // ── Reviews RecyclerView ─────────────────────────────────────────────
        reviewList    = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setNestedScrollingEnabled(false);
        rvReviews.setAdapter(reviewAdapter);

        loadReviews(id);

        // ── Leave review button ──────────────────────────────────────────────
        btnLeaveReview.setOnClickListener(v -> {
            LeaveReviewDialogFragment dialog = LeaveReviewDialogFragment.newInstance(id);
            dialog.setOnReviewSubmittedListener(this);
            dialog.show(getSupportFragmentManager(), "LeaveReviewDialog");
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupThumbClick(ImageView thumb, String fileName) {
        thumb.setOnClickListener(v -> {
            ImageView ivMainImage = findViewById(R.id.ivMainImage);
            String oldmain = currentMainFileName;
            currentMainFileName = fileName;
            Glide.with(this)
                    .load(DatabaseManager.PREVIEW_URL + currentMainFileName)
                    .into(ivMainImage);
            Glide.with(this)
                    .load(DatabaseManager.PREVIEW_URL + oldmain)
                    .into(thumb);
            setupThumbClick(thumb, oldmain);
        });
    }

    private void loadReviews(int makeoverId) {
        DatabaseManager.fetchReviews(makeoverId, new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                reviewList.clear();
                // show last 3 reviews per the layout design
                int limit = Math.min(response.length(), 3);
                for (int i = 0; i < limit; i++) {
                    try {
                        reviewList.add(Review.fromJson(response.getJSONObject(i)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                reviewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String message) {
                // reviews are non-critical — fail silently
            }
        });
    }

    @Override
    public void onReviewSubmitted() {
        int id = getIntent().getIntExtra("MAKEOVER_ID", -1);
        loadReviews(id);
    }
}
