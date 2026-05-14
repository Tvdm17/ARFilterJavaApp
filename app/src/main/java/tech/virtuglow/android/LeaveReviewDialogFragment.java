package tech.virtuglow.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

public class LeaveReviewDialogFragment extends DialogFragment {

    public interface OnReviewSubmitted {
        void onReviewSubmitted();
    }

    private static final String ARG_MAKEOVER_ID = "makeover_id";
    private OnReviewSubmitted listener;

    public static LeaveReviewDialogFragment newInstance(int makeoverId) {
        LeaveReviewDialogFragment f = new LeaveReviewDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MAKEOVER_ID, makeoverId);
        f.setArguments(args);
        return f;
    }

    public void setOnReviewSubmittedListener(OnReviewSubmitted listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leave_review_dialog, container, false);
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

        RatingBar rbUserRating   = view.findViewById(R.id.rbUserRating);
        EditText  etReviewComment = view.findViewById(R.id.etReviewComment);
        Button    btnSubmit       = view.findViewById(R.id.btnSubmitReview);
        Button    btnCancel       = view.findViewById(R.id.btnCancelReview);

        btnCancel.setOnClickListener(v -> dismiss());

        btnSubmit.setOnClickListener(v -> {
            float rating  = rbUserRating.getRating();
            String comment = etReviewComment.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            if (comment.isEmpty()) {
                Toast.makeText(getContext(), "Please write a comment", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);

            DatabaseManager.addReview(makeoverId, DatabaseManager.getUserid(), rating, comment,
                    new DatabaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Review submitted!", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onReviewSubmitted();
                            dismiss();
                        }

                        @Override
                        public void onFailure(String message) {
                            if (getContext() == null) return;
                            btnSubmit.setEnabled(true);
                            Toast.makeText(getContext(), "Failed to submit: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
