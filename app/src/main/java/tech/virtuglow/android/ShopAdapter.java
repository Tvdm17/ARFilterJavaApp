package tech.virtuglow.android;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * RecyclerView adapter for the Shop screen.
 * Binds a List<ShopItem> to item_shop.xml cards.
 *
 * Each card shows:
 *   - item image (placeholder until Glide is added)
 *   - item name
 *   - "View" button — opens PreviewActivity with the makeover's DeepAR filter
 *   - star RatingBar showing average rating
 *   - numeric rating tooltip (hidden by default, toggled by tapping the stars)
 */
public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

    private final Context context;
    private final List<ShopItem> items;

    public ShopAdapter(Context context, List<ShopItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShopItem item = items.get(position);

        Log.d("ADAPTER_DATA", "Binding item: " + item.getName() + " with: " + item.getAverageRating());

        holder.tvItemName.setText(item.getName());
        holder.rbRating.setRating((float) item.getAverageRating());
        holder.tvRatingValue.setText(String.valueOf(item.getAverageRating()));

        String fullImageurl = DatabaseManager.PREVIEW_URL + item.getPreviewImage();


        // Image loading — add Glide to build.gradle, then replace this with:
        Glide.with(context)
                .load(fullImageurl)
                //.placeholder(R.drawable.loading_spinner) add a loading thingie same as customerview
                //.error(R.drawable.default_something) // default if error
                .into(holder.ivItemImage);

        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItemCardActivity.class);
            intent.putExtra("MAKEOVER_ID", item.getId());
            // extra flag for pictures only ? how was this implemented ?
            intent.putExtra("CAMERA_MODE", "PICTURE_ONLY");
            context.startActivity(intent);
        });

        holder.ivItemImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItemCardActivity.class);
            intent.putExtra("MAKEOVER_ID", item.getId());
            context.startActivity(intent);
        });

        holder.rbRating.setOnClickListener(v ->
            holder.tvRatingValue.setVisibility(
                holder.tvRatingValue.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            )
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName;
        Button btnView;
        RatingBar rbRating;
        TextView tvRatingValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            btnView = itemView.findViewById(R.id.btnView);
            rbRating = itemView.findViewById(R.id.rbRating);
            tvRatingValue = itemView.findViewById(R.id.tvRatingValue);
        }
    }
}
