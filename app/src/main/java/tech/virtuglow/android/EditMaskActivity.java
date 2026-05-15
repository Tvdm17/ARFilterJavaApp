package tech.virtuglow.android;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditMaskActivity extends DrawerMenu {

    private EditText  etMaskName;
    private EditText etMaskPrice;
    private ChipGroup chipGroupTags;
    private EditText  etNewTag;
    private TextView  tvDeepArFileName;
    private TextView  tvTagSuggestions;
    private Uri deepArFileUri = null;

    // thumbnail previews: index 0 = main, 1–4 = secondary slots
    private final ImageView[] thumbViews = new ImageView[5];

    private String[] serverImageNames = new String[5]; // To store the 5 names from PHP

    private String serverFileName = ""; // var to store filename returned buy php

    private String serverDeepArName ="";//name of deeparfile

    private final List<String> originalTags = new ArrayList<>();
    private final List<String> currentTags  = new ArrayList<>();

    private int activeSlot = 0;

    // single launcher for all 5 image/video slots
    private final ActivityResultLauncher<Intent> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri mediaUri = result.getData().getData();
                    if (mediaUri != null && thumbViews[activeSlot] != null) {
                        Glide.with(this).load(mediaUri).into(thumbViews[activeSlot]);
                        // TODO: upload to server once upload_makeover_preview endpoint is available
                        startImageUpload(mediaUri, activeSlot); // php image upload
                        // optional, add names of files to thumbview, just use getFilename(uri)
                    }
                }
            });

    private final ActivityResultLauncher<Intent> deepArPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    deepArFileUri = result.getData().getData();
                    if (deepArFileUri != null) {
                        //String path = deepArFileUri.getLastPathSegment();
                        // use the new method to get the name instead
                        tvDeepArFileName.setText(getFileName(deepArFileUri));

                        startDeepArUpload(deepArFileUri);// php deepar upload
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

        etMaskName        = findViewById(R.id.etMaskName);
        etMaskPrice       = findViewById(R.id.etMaskPrice);
        chipGroupTags     = findViewById(R.id.chipGroupTags);
        etNewTag          = findViewById(R.id.etNewTag);
        tvDeepArFileName  = findViewById(R.id.tvDeepArFileName);
        tvTagSuggestions  = findViewById(R.id.tvTagSuggestions);

        loadPopularTags();

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


        //Price
        TextView tvPriceLabel = findViewById(R.id.tvPriceLabel);



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
            String price = etMaskPrice.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a mask name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (price.isEmpty()) {
                Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isvalidprice(price)) {
                Toast.makeText(this, "Invalid price format, please enter a valid price.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (serverImageNames[0] == null || serverDeepArName.isEmpty() || !serverDeepArName.toLowerCase().endsWith(".deepar")) {
                Toast.makeText(this, "Main image and DeepAR file are required", Toast.LENGTH_SHORT).show();
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
                DatabaseManager.updateMakeover(makeoverId, name, price, serverImageNames[0], serverDeepArName, new DatabaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {

                        DatabaseManager.clearAndLinkImages(makeoverId, serverImageNames, new DatabaseManager.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        syncTags(makeoverId);
                                        Toast.makeText(EditMaskActivity.this, "Mask updated!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(String message) {} // handle error
                                });
                    }

                    @Override
                    public void onFailure(String message) {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditMaskActivity.this, "Update failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                DatabaseManager.createMakeover(DatabaseManager.getUserid(), name,  price, serverImageNames[0], serverDeepArName, new DatabaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {

                        DatabaseManager.getLatestMakeoverId(DatabaseManager.getUserid(), new DatabaseManager.IdCallback() {
                            @Override
                            public void onSuccess(int id) {
                                for(int i =1; i < serverImageNames.length; i++){
                                    if(serverImageNames[i] != null && !serverImageNames[i].isEmpty()){
                                        DatabaseManager.addImageToMakeover(serverImageNames[i],
                                        id,
                                        new DatabaseManager.SimpleCallback() {
                                            @Override
                                            public void onSuccess() { } // image linked
                                            @Override
                                            public void onFailure(String message) {} // errormessage
                                        });
                                    }
                                }

                                syncTags(id);
                                Toast.makeText(EditMaskActivity.this, "Makeover fully created.", Toast.LENGTH_SHORT).show();
                                finish();

                            }

                            @Override
                            public void onFailure(String message) {
                                btnSave.setEnabled(true);
                                Toast.makeText(EditMaskActivity.this, "ID Lookup failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(String message) {

                        btnSave.setEnabled(true);
                        Toast.makeText(EditMaskActivity.this, "save failed" + message,Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean isvalidprice (String price) {
        try {
            double doubleprice = Double.parseDouble(price);
            return doubleprice >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getFileName(Uri uri){
//  When you pick a file using a URI in Android,
//  you don't get a direct file path like C:/Downloads/file.jpg
//  you get a reference to a database entry managed by the Android system
//  the cursor allows you to "read" the metadata of that file, like its name and size
    String fileName = null;
    try {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameindex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if(nameindex != -1){fileName = cursor.getString(nameindex);}
            //fileName = cursor.getString(nameindex);
            cursor.close();
            }
        }
    catch (Exception e){
        e.printStackTrace();
        }
    // fallback:
    if(fileName == null){
        fileName = uri.getPath();
        int cut = fileName.lastIndexOf('/');
        if(cut != -1){fileName = fileName.substring(cut + 1);}
        }
    // return filename
    return fileName;
    }


    private void startImageUpload(Uri uri, int slot) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = getBytes(is);
            String name = getFileName(uri);

            if (name == null || !(name.toLowerCase().endsWith(".jpg") ||
                    name.toLowerCase().endsWith(".jpeg") ||
                    name.toLowerCase().endsWith(".png"))) {

                Toast.makeText(this, "Please select a valid image (JPG or PNG)", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseManager.uploadPreviewImage(bytes, name, new DatabaseManager.UploadCallback() {
                @Override
                public void onSuccess(String fileNameFromServer) {
                    serverImageNames[slot] = fileNameFromServer; // Store image name
                    runOnUiThread(() -> Toast.makeText(EditMaskActivity.this, "Slot: " +slot +" Preview image ready", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> Toast.makeText(EditMaskActivity.this, "Image Error: " + error, Toast.LENGTH_SHORT).show());
                    Log.d("image error", error);
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void startDeepArUpload(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = getBytes(is);
            String name = getFileName(uri);

            if (name == null || !name.toLowerCase().endsWith(".deepar")) {
                Toast.makeText(this, "Invalid format!", Toast.LENGTH_SHORT).show();

                // CRITICAL FIX FOR EDIT MODE:
                // We must clear the old valid name so the user can't save
                // the mask while a "bad" file is currently selected in the picker.
                serverDeepArName = "";

                tvDeepArFileName.setText("INVALID FILE SELECTED");
                return;
            }




            DatabaseManager.uploadDeepArFile(bytes, name, new DatabaseManager.UploadCallback() {
                @Override
                public void onSuccess(String fileNameFromServer) {
                    serverDeepArName = fileNameFromServer; // Store .deepar name
                    runOnUiThread(() -> Toast.makeText(EditMaskActivity.this, "DeepAR file ready", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> Toast.makeText(EditMaskActivity.this, "DeepAR Error: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    public byte[] getBytes(InputStream input) throws IOException {

        ByteArrayOutputStream bytebuffer = new ByteArrayOutputStream();
        int buffersize = 1024;
        byte[] buffer =  new byte[buffersize];
        int len  = 0;
        while((len = input.read(buffer)) != -1 ){
            bytebuffer.write(buffer, 0, len);
        }
        return bytebuffer.toByteArray();

    }

    private void prefillFields(int makeoverId) {
        Makeover item = DatabaseManager.getMakeoverById(makeoverId);
        if (item == null) return;

        etMaskName.setText(item.getName());
        etMaskPrice.setText(String.valueOf(item.getPrice()));
        tvDeepArFileName.setText(item.getDeeparFileName());
        serverImageNames[0] = item.getPreviewImage();
        serverDeepArName = item.getDeeparFileName();

        if (item.isTagsLoaded()) {
            originalTags.addAll(item.getTags());
            for (String tag : item.getTags()) {
                addTag(tag);
            }
        }

        Glide.with(this)
                .load(DatabaseManager.PREVIEW_URL + item.getPreviewImage())
                .into(thumbViews[0]);
        DatabaseManager.fetchSecondaryImages(item.getId(), new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                try{
                    for(int i = 1; i < response.length() + 1 && i < thumbViews.length; i++ ){
                        String fileName = response.getJSONObject(i-1).getString("fileName");
                        serverImageNames[i] = fileName;
                        Glide.with(EditMaskActivity.this).load(DatabaseManager.PREVIEW_URL + fileName).into(thumbViews[i]);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String message) {
                // handle error opt.
            }
        });
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

    private void loadPopularTags() {
        DatabaseManager.fetchPopularTags(new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                List<String> suggestions = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    JSONObject obj = response.optJSONObject(i);
                    if (obj != null) {
                        String tag = obj.optString("tagName", "");
                        if (!tag.isEmpty()) suggestions.add(tag);
                    }
                }
                if (suggestions.isEmpty()) return;

                String prefix = "popular: ";
                String joined = prefix + TextUtils.join(", ", suggestions);
                SpannableString ss = new SpannableString(joined);
                int start = prefix.length();
                for (String tag : suggestions) {
                    int end = start + tag.length();
                    final String t = tag;
                    ss.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) { addTag(t); }
                        @Override
                        public void updateDrawState(TextPaint ds) {
                            ds.setColor(ContextCompat.getColor(EditMaskActivity.this, R.color.colorPrimary));
                            ds.setUnderlineText(false);
                        }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = end + 2;
                }

                tvTagSuggestions.setText(ss);
                tvTagSuggestions.setMovementMethod(LinkMovementMethod.getInstance());
                tvTagSuggestions.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String message) {}
        });
    }

    private void addTag(String tagText) {
        if (tagText.isEmpty()) return;
        if (currentTags.contains(tagText)) {
            Toast.makeText(this, "Tag already added", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTags.add(tagText);
        Chip chip = new Chip(this);
        chip.setText(tagText);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.backgroundSecondary);
        chip.setChipStrokeColorResource(R.color.colorPrimary);
        chip.setChipStrokeWidth(1f);
        chip.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        chip.setOnCloseIconClickListener(v -> {
            currentTags.remove(tagText);
            chipGroupTags.removeView(chip);
        });
        chipGroupTags.addView(chip);
        etNewTag.setText("");
    }

    private void syncTags(int makeoverId) {
        List<String> toAdd = new ArrayList<>(currentTags);
        toAdd.removeAll(originalTags);

        List<String> toRemove = new ArrayList<>(originalTags);
        toRemove.removeAll(currentTags);

        for (String tag : toAdd) {
            DatabaseManager.addTagToMakeover(makeoverId, tag, new DatabaseManager.SimpleCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String message) {}
            });
        }
        for (String tag : toRemove) {
            DatabaseManager.removeTagFromMakeover(makeoverId, tag, new DatabaseManager.SimpleCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String message) {}
            });
        }
    }
}
