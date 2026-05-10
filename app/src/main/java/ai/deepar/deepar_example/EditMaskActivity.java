package ai.deepar.deepar_example;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditMaskActivity extends DrawerMenu {

    private EditText  etMaskName;
    private ChipGroup chipGroupTags;
    private EditText  etNewTag;
    private TextView  tvDeepArFileName;
    private Uri       deepArFileUri = null;

    // thumbnail previews: index 0 = main, 1–4 = secondary slots
    private final ImageView[] thumbViews = new ImageView[5];

    private String[] serverImageNames = new String[5]; // To store the 5 names from PHP

    private String serverFileName = ""; // var to store filename returned buy php

    private String serverDeepArName ="";//name of deeparfile

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
            if (serverImageNames[0] == null || serverDeepArName.isEmpty()) {
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
//                DatabaseManager.updateMakeover(makeoverId, name, serverImageNames[0], serverDeepArName, new DatabaseManager.SimpleCallback() {
//                    @Override
//                    public void onSuccess() {
//                        Toast.makeText(EditMaskActivity.this, "Mask updated!", Toast.LENGTH_SHORT).show();
//                        finish();
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        btnSave.setEnabled(true);
//                        Toast.makeText(EditMaskActivity.this, "Update failed: " + message, Toast.LENGTH_SHORT).show();
//                    }
//                });
                DatabaseManager.updateMakeover(makeoverId, name, serverFileName, onDone); // old method, new on top
            } else {
                DatabaseManager.createMakeover(DatabaseManager.getUserid(), name, serverImageNames[0], serverDeepArName, new DatabaseManager.SimpleCallback() {
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
