package ai.deepar.deepar_example;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class CreatorItemCardActivity extends DrawerMenu {

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

        // ── Top bar ──────────────────────────────────────────────────────────
        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        TextView tvMaskName = findViewById(R.id.tvMaskName);
        tvMaskName.setText(item.getName());

        // ── Main image ───────────────────────────────────────────────────────
        ImageView ivMainImage = findViewById(R.id.ivMainImage);
        Glide.with(this)
                .load(DatabaseManager.PREVIEW_URL + item.getPreviewImage())
                .into(ivMainImage);

        // ── Thumbnails (swap main image on tap) ──────────────────────────────
        ImageView ivThumb1 = findViewById(R.id.ivThumb1);
        ImageView ivThumb2 = findViewById(R.id.ivThumb2);
        ImageView ivThumb3 = findViewById(R.id.ivThumb3);
        ImageView ivThumb4 = findViewById(R.id.ivThumb4);

        ivThumb1.setOnClickListener(v -> swapMainImage(ivMainImage, ivThumb1));
        ivThumb2.setOnClickListener(v -> swapMainImage(ivMainImage, ivThumb2));
        ivThumb3.setOnClickListener(v -> swapMainImage(ivMainImage, ivThumb3));
        ivThumb4.setOnClickListener(v -> swapMainImage(ivMainImage, ivThumb4));

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

    private void swapMainImage(ImageView main, ImageView thumb) {
        Drawable mainDrawable  = main.getDrawable();
        Drawable thumbDrawable = thumb.getDrawable();
        main.setImageDrawable(thumbDrawable);
        thumb.setImageDrawable(mainDrawable);
    }
}
