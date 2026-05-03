package ai.deepar.deepar_example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

public class ItemCardActivity extends AppCompatActivity {

    public ShopItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_card);

        item = (ShopItem)getIntent().getSerializableExtra("shopItem");

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_shop) {
                Intent intent = new Intent(this, ShopActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            if (menuItem.getItemId() == R.id.nav_customer_home) {
                Intent intent = new Intent(this, CustomerHomeActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        //DO THIS! GET USERNAME FROM DB
        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(DatabaseManager.getUsername());

        TextView tvMaskName = findViewById(R.id.tvMaskName);
        tvMaskName.setText(item.getName());

        ImageView ivMainImage = findViewById(R.id.ivMainImage);
        ImageView ivThumb1 = findViewById(R.id.ivThumb1);
        ImageView ivThumb2 = findViewById(R.id.ivThumb2);
        ImageView ivThumb3 = findViewById(R.id.ivThumb3);
        ImageView ivThumb4 = findViewById(R.id.ivThumb4);//DO FOR THE OTHERS??

        Glide.with(this).load(DatabaseManager.PREVIEW_URL + item.getPreviewImage()).into(ivMainImage);
        Glide.with(this).load(item.getImageUrl(1)).into(ivThumb1); //need to add extra images
        Glide.with(this).load(item.getImageUrl(2)).into(ivThumb2); //need to add extra images
        Glide.with(this).load(item.getImageUrl(3)).into(ivThumb3); //need to add extra images
        Glide.with(this).load(item.getImageUrl(4)).into(ivThumb4); //need to add extra images


        Button btnAddDelete = findViewById(R.id.btnAddDelete);
        btnAddDelete.setOnClickListener(v -> {
            //ADD CODE
        });

        RatingBar rbAverageRating = findViewById(R.id.rbAverageRating);
        rbAverageRating.setRating((float) item.getAverageRating());

        TextView tvAverageValue = findViewById(R.id.tvAverageValue);
        tvAverageValue.setText(String.format("%.1f / 5", item.getAverageRating()));

        RecyclerView rvReviews = findViewById(R.id.rvReviews); // with last 3 reviews
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setNestedScrollingEnabled(false);
        rvReviews.setAdapter(new ReviewAdapter(last3Reviews)); //TODO


        Button btnLeaveReview = findViewById(R.id.btnLeaveReview); //visible to owners only
        btnLeaveReview.setOnClickListener(v -> {
            LeaveReviewDialogFragment dialog = new LeaveReviewDialogFragment(); //TODO
            dialog.show(getSupportFragmentManager(), "LeaveReviewDialog");
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}