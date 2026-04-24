package ai.deepar.deepar_example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter popup shown when the user taps the filter icon in ShopActivity.
 * Displays category chips (averagelook / natural / medium / bright) that map to
 * the `group` column in the `tag` table.
 *
 * When DB is ready, replace the hardcoded chips in fragment_filter_dialog.xml with
 * chips built dynamically from:
 *   SELECT DISTINCT `group` FROM tag ORDER BY `group`
 *
 * Usage from ShopActivity:
 *   FilterDialogFragment dialog = new FilterDialogFragment();
 *   dialog.setOnFiltersAppliedListener(this);   // ShopActivity implements OnFiltersApplied
 *   dialog.show(getSupportFragmentManager(), "FilterDialog");
 */
public class FilterDialogFragment extends DialogFragment {

    /**
     * Callback interface — ShopActivity implements this to receive the list of
     * selected category names when the user taps Apply.
     */
    public interface OnFiltersApplied {
        void onFiltersApplied(List<String> selectedCategories);
    }

    private OnFiltersApplied listener;

    public void setOnFiltersAppliedListener(OnFiltersApplied listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupFilters);
        Button btnClear = view.findViewById(R.id.btnClearFilter);
        Button btnApply = view.findViewById(R.id.btnApplyFilter);

        // Clear deselects all chips without closing the dialog
        btnClear.setOnClickListener(v -> chipGroup.clearCheck());

        btnApply.setOnClickListener(v -> {
            // Collect the text of every checked chip — these are the selected category names
            List<String> selected = new ArrayList<>();
            for (int id : chipGroup.getCheckedChipIds()) {
                Chip chip = view.findViewById(id);
                if (chip != null) selected.add(chip.getText().toString());
            }
            if (listener != null) listener.onFiltersApplied(selected);
            dismiss();
        });
    }
}
