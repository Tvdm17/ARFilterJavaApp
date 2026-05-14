package tech.virtuglow.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class AllReviewsDialogFragment extends DialogFragment {

    private static final String ARG_MAKEOVER_ID = "makeover_id";

    public static AllReviewsDialogFragment newInstance(int makeoverId) {
        AllReviewsDialogFragment f = new AllReviewsDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MAKEOVER_ID, makeoverId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_reviews_dialog, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int makeoverId = getArguments() != null ? getArguments().getInt(ARG_MAKEOVER_ID, -1) : -1;

        RecyclerView rvAllReviews    = view.findViewById(R.id.rvAllReviews);
        RatingBar    rbAverageRating = view.findViewById(R.id.rbAverageRating);
        TextView     tvAverageRating = view.findViewById(R.id.tvAverageRating);

        List<Review> reviewList = new ArrayList<>();
        ReviewAdapter adapter = new ReviewAdapter(getContext(), reviewList);
        rvAllReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllReviews.setAdapter(adapter);

        view.findViewById(R.id.btnCloseReviews).setOnClickListener(v -> dismiss());

        if (makeoverId == -1) return;

        DatabaseManager.fetchReviews(makeoverId, new DatabaseManager.APICallback() {
            @Override
            public void onSuccess(JSONArray response) {
                if (getContext() == null) return;
                reviewList.clear();
                float ratingSum = 0;
                for (int i = 0; i < response.length(); i++) {
                    try {
                        Review r = Review.fromJson(response.getJSONObject(i));
                        reviewList.add(r);
                        ratingSum += r.getRating();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                adapter.notifyDataSetChanged();

                float avg = reviewList.isEmpty() ? 0 : ratingSum / reviewList.size();
                rbAverageRating.setRating(avg);
                tvAverageRating.setText(String.format(Locale.getDefault(), "  %.1f / 5", avg));
            }

            @Override
            public void onFailure(String message) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Could not load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
