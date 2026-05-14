package tech.virtuglow.android;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class FilterDialogFragment extends DialogFragment {

    public interface OnFiltersApplied {
        void onFiltersApplied(List<String> selectedCategories);
    }

    private OnFiltersApplied listener;
    private List<String> availableTags = new ArrayList<>();
    private List<String> preSelectedTags = new ArrayList<>();

    public void setOnFiltersAppliedListener(OnFiltersApplied listener) {
        this.listener = listener;
    }

    public void setAvailableTags(List<String> tags) {
        this.availableTags = new ArrayList<>(tags);
    }

    public void setPreSelectedTags(List<String> tags) {
        this.preSelectedTags = new ArrayList<>(tags);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_dialog, container, false);
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

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupFilters);
        Button btnClear = view.findViewById(R.id.btnClearFilter);
        Button btnApply = view.findViewById(R.id.btnApplyFilter);

        buildChips(chipGroup);

        btnClear.setOnClickListener(v -> chipGroup.clearCheck());

        btnApply.setOnClickListener(v -> {
            List<String> selected = new ArrayList<>();
            for (int id : chipGroup.getCheckedChipIds()) {
                Chip chip = view.findViewById(id);
                if (chip != null) selected.add(chip.getText().toString());
            }
            if (listener != null) listener.onFiltersApplied(selected);
            dismiss();
        });
    }

    private void buildChips(ChipGroup chipGroup) {
        chipGroup.removeAllViews();

        if (availableTags.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No filters available.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            chipGroup.addView(empty);
            return;
        }

        int heightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        float strokePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        ColorStateList bgColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.backgroundSecondary));
        ColorStateList accentColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary));

        for (String tag : availableTags) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(tag);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            chip.setMinHeight(heightPx);
            chip.setCheckable(true);
            chip.setChecked(preSelectedTags.contains(tag));
            chip.setChipBackgroundColor(bgColor);
            chip.setCheckedIconTint(accentColor);
            chip.setChipStrokeColor(accentColor);
            chip.setChipStrokeWidth(strokePx);
            chipGroup.addView(chip);
        }
    }
}
