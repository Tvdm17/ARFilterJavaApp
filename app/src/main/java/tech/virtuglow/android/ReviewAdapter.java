package tech.virtuglow.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final Context context;
    private final List<Review> reviews;

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.tvReviewerName.setText(review.getAuthorName());
        holder.rbReviewRating.setRating(review.getRating());
        holder.tvReviewText.setText(review.getComment());

        holder.tvReadMore.setOnClickListener(v -> {
            if (holder.tvReviewText.getMaxLines() == 2) {
                holder.tvReviewText.setMaxLines(Integer.MAX_VALUE);
                holder.tvReadMore.setText("less");
            } else {
                holder.tvReviewText.setMaxLines(2);
                holder.tvReadMore.setText("more");
            }
        });

        // hide "more" button if the review text fits within 2 lines
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
        final TextView  tvReviewerName;
        final RatingBar rbReviewRating;
        final TextView  tvReviewText;
        final TextView  tvReadMore;

        ViewHolder(View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            rbReviewRating = itemView.findViewById(R.id.rbReviewRating);
            tvReviewText   = itemView.findViewById(R.id.tvReviewText);
            tvReadMore     = itemView.findViewById(R.id.tvReadMore);
        }
    }
}
