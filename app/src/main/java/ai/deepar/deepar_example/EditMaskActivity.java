package ai.deepar.deepar_example;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class EditMaskActivity extends DrawerMenu {

    private EditText  etMaskName;
    private ChipGroup chipGroupTags;
    private EditText  etNewTag;
    private TextView  tvDeepArFileName;
    private Uri       deepArFileUri = null;

    // thumbnail previews: index 0 = main, 1–4 = secondary slots
    private final ImageView[] thumbViews = new ImageView[5];
    private int activeSlot = 0;

    // single launcher for all 5 image/video slots
    private final ActivityResultLauncher<Intent> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri mediaUri = result.getData().getData();
                    if (mediaUri != null && thumbViews[activeSlot] != null) {
                        Glide.with(this).load(mediaUri).into(thumbViews[activeSlot]);
                        // TODO: upload to server once upload_makeover_preview endpoint is available
                    }
                }
            });

    private final ActivityResultLauncher<Intent> deepArPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    deepArFileUri = result.getData().getData();
                    if (deepArFileUri != null) {
                        String path = deepArFileUri.getLastPathSegment();
                        tvDeepArFileName.setText(path != null ? path : deepArFileUri.toString());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_mask);

        startDrawer();

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        etMaskName      = findViewById(R.id.etMaskName);
        chipGroupTags   = findViewById(R.id.chipGroupTags);
        etNewTag        = findViewById(R.id.etNewTag);
        tvDeepArFileName = findViewById(R.id.tvDeepArFileName);

        thumbViews[0] = findViewById(R.id.ivThumbMain);
        thumbViews[1] = findViewById(R.id.ivThumb1);
        thumbViews[2] = findViewById(R.id.ivThumb2);
        thumbViews[3] = findViewById(R.id.ivThumb3);
        thumbViews[4] = findViewById(R.id.ivThumb4);

        // ── DeepAR file picker ───────────────────────────────────────────────
        findViewById(R.id.btnPickDeepAr).setOnClickListener(v -> openDeepArPicker());

        // ── Media pickers ────────────────────────────────────────────────────
        findViewById(R.id.btnPickMain).setOnClickListener(v -> openMediaPicker(0));
        findViewById(R.id.btnPick1).setOnClickListener(v -> openMediaPicker(1));
        findViewById(R.id.btnPick2).setOnClickListener(v -> openMediaPicker(2));
        findViewById(R.id.btnPick3).setOnClickListener(v -> openMediaPicker(3));
        findViewById(R.id.btnPick4).setOnClickListener(v -> openMediaPicker(4));

        // ── Tag input ────────────────────────────────────────────────────────
        findViewById(R.id.btnAddTag).setOnClickListener(v ->
                addTag(etNewTag.getText().toString().trim()));

        etNewTag.setOnEditorActionListener((v, actionId, event) -> {
            addTag(etNewTag.getText().toString().trim());
            return true;
        });

        // ── Edit vs Create mode ──────────────────────────────────────────────
        int makeoverId = getIntent().getIntExtra("MAKEOVER_ID", -1);
        boolean isEditMode = (makeoverId != -1);

        if (isEditMode) {
            prefillFields(makeoverId);
        }

        // ── Save button ──────────────────────────────────────────────────────
        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            String name = etMaskName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a mask name", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSave.setEnabled(false);

            DatabaseManager.SimpleCallback onDone = new DatabaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EditMaskActivity.this,
                            isEditMode ? "Mask updated!" : "Mask created!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditMaskActivity.this, "Save failed: " + message, Toast.LENGTH_SHORT).show();
                }
            };

            if (isEditMode) {
                DatabaseManager.updateMakeover(makeoverId, name, onDone);
            } else {
                DatabaseManager.createMakeover(DatabaseManager.getUserid(), name, onDone);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void prefillFields(int makeoverId) {
        Makeover item = DatabaseManager.getMakeoverById(makeoverId);
        if (item == null) return;

        etMaskName.setText(item.getName());

        if (item.isTagsLoaded()) {
            for (String tag : item.getTags()) {
                addTag(tag);
            }
        }

        Glide.with(this)
                .load(DatabaseManager.PREVIEW_URL + item.getPreviewImage())
                .into(thumbViews[0]);
    }

    private void openDeepArPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        deepArPickerLauncher.launch(intent);
    }

    private void openMediaPicker(int slot) {
        activeSlot = slot;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        mediaPickerLauncher.launch(intent);
    }

    private void addTag(String tagText) {
        if (tagText.isEmpty()) return;

        Chip chip = new Chip(this);
        chip.setText(tagText);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.backgroundSecondary);
        chip.setChipStrokeColorResource(R.color.colorPrimary);
        chip.setChipStrokeWidth(1f);
        chip.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        chip.setOnCloseIconClickListener(v -> chipGroupTags.removeView(chip));
        chipGroupTags.addView(chip);
        etNewTag.setText("");
    }
}
