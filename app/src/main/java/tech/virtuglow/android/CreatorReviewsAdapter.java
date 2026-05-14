package tech.virtuglow.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CreatorReviewsAdapter extends RecyclerView.Adapter<CreatorReviewsAdapter.ViewHolder> {

    private final List<Review> reviews;

    public CreatorReviewsAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_creator_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review r = reviews.get(position);

        holder.tvMaskName.setText(r.getMaskName());
        holder.tvReviewerName.setText(r.getAuthorName());
        holder.rbReviewRating.setRating(r.getRating());
        holder.tvReviewText.setText(r.getComment());

        holder.tvReviewText.setMaxLines(2);
        holder.tvReadMore.setText(R.string.read_more);

        holder.tvReadMore.setOnClickListener(v -> {
            if (holder.tvReviewText.getMaxLines() == 2) {
                holder.tvReviewText.setMaxLines(Integer.MAX_VALUE);
                holder.tvReadMore.setText("less");
            } else {
                holder.tvReviewText.setMaxLines(2);
                holder.tvReadMore.setText(R.string.read_more);
            }
        });

        holder.tvReviewText.post(() -> {
            if (holder.tvReviewText.getLineCount() <= 2) {
                holder.tvReadMore.setVisibility(View.GONE);
            } else {
                holder.tvReadMore.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView  tvMaskName;
        final TextView  tvReviewerName;
        final RatingBar rbReviewRating;
        final TextView  tvReviewText;
        final TextView  tvReadMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaskName     = itemView.findViewById(R.id.tvMaskName);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            rbReviewRating = itemView.findViewById(R.id.rbReviewRating);
            tvReviewText   = itemView.findViewById(R.id.tvReviewText);
            tvReadMore     = itemView.findViewById(R.id.tvReadMore);
        }
    }
}
