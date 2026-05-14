package tech.virtuglow.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import org.json.JSONArray;

public class CreatorItemCardActivity extends DrawerMenu {

    private String currentMainFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_creator_item_card);

        startDrawer();

        int id = getIntent().getIntExtra("MAKEOVER_ID", -1);
        Makeover item = DatabaseManager.getMakeoverById(id);

        if (item == null) {
            Toast.makeText(this, "Data sync error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentMainFileName = item.getPreviewImage();

        // ── Top bar ──────────────────────────────────────────────────────────
        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        TextView tvMaskName = findViewById(R.id.tvMaskName);
        tvMaskName.setText(item.getName());

        // ── Main image ───────────────────────────────────────────────────────
        ImageView ivMainImage = findViewById(R.id.ivMainImage);


        ImageView[] thumbs = {
                findViewById(R.id.ivThumb1),
                findViewById(R.id.ivThumb2),
                findViewById(R.id.ivThumb3),
                findViewById(R.id.ivThumb4)
        };

        Glide.with(this).load(DatabaseManager.PREVIEW_URL + item.getPreviewImage()).into(ivMainImage);

        // load secondary images
        DatabaseManager.fetchSecondaryImages(item.getId(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                try {
                    for (int i = 0; i < response.length() && i < thumbs.length; i++) {
                        String fileName = response.getJSONObject(i).getString("fileName");
                        String fullUrl = DatabaseManager.PREVIEW_URL + fileName;

                        Glide.with(CreatorItemCardActivity.this)
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

        // ── Edit button → EditMaskActivity (edit mode) ───────────────────────
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMaskActivity.class);
            intent.putExtra("MAKEOVER_ID", id);
            startActivity(intent);
        });

        // ── Reviews row → AllReviewsDialogFragment ───────────────────────────
        findViewById(R.id.rowReviews).setOnClickListener(v -> {
            AllReviewsDialogFragment dialog = AllReviewsDialogFragment.newInstance(id);
            dialog.show(getSupportFragmentManager(), "AllReviewsDialog");
        });

        // ── Metrics row → PurchaseMetricsDialogFragment ──────────────────────
        findViewById(R.id.rowMetrics).setOnClickListener(v -> {
            PurchaseMetricsDialogFragment dialog = PurchaseMetricsDialogFragment.newInstance(
                    id, (float) item.getAverageRating());
            dialog.show(getSupportFragmentManager(), "PurchaseMetricsDialog");
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
}
