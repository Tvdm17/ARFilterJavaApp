package tech.virtuglow.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PurchaseMetricsDialogFragment extends DialogFragment {

    private static final String ARG_MAKEOVER_ID  = "makeover_id";
    private static final String ARG_AVG_RATING   = "avg_rating";


    public static PurchaseMetricsDialogFragment newInstance(int makeoverId, float avgRating) {

        PurchaseMetricsDialogFragment f = new PurchaseMetricsDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MAKEOVER_ID, makeoverId);
        args.putFloat(ARG_AVG_RATING, avgRating);

        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_purchase_metrics_dialog, container, false);
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

        float avgRating = getArguments() != null ? getArguments().getFloat(ARG_AVG_RATING, 0f) : 0f;

        android.widget.TextView tvAvgValue = view.findViewById(R.id.tvAvgRatingValue);
        tvAvgValue.setText(String.format(Locale.getDefault(), "%.1f / 5", avgRating));

        initEmptyChart(view.findViewById(R.id.chartReviews));
        int currentId = getArguments().getInt(ARG_MAKEOVER_ID);
        initData(currentId, view);


        view.findViewById(R.id.btnCloseMetrics).setOnClickListener(v -> dismiss());
    }

    private void initEmptyChart(LineChart chart) {
        chart.setNoDataText("Data coming soon");
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }
    private void initData(int makeoverID, View view){

        DatabaseManager.fetchRemoves(String.valueOf(makeoverID) ,new DatabaseManager.APICallback() {

            @Override
            public void onSuccess(JSONArray response) {
                int activeCount = 0;
                int removedCount = 0;

                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        if (obj.getInt("Removed") == 0) activeCount = obj.getInt("total");
                        else removedCount = obj.getInt("total");
                    }

                    setupBarChart(view.findViewById(R.id.metrics), activeCount, removedCount);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(String message) {}
        });

    }
    private void setupBarChart(HorizontalBarChart chart, int active, int removed) {
        List<BarEntry> entries = new ArrayList<>();
        // 0 = Active, 1 = Removed
        entries.add(new BarEntry(0f, active));
        entries.add(new BarEntry(1f, removed));

        BarDataSet dataSet = new BarDataSet(entries, "Purchase Stats");
        dataSet.setColors(new int[]{android.graphics.Color.GREEN, android.graphics.Color.RED});
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        chart.setData(data);

        // ui
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getDescription().setEnabled(false);
        // Disable the right Y-axis (the one currently showing nothing at the bottom)
        chart.getAxisRight().setEnabled(false);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setEnabled(false);


        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false); // Removes the vertical lines
        xAxis.setDrawAxisLine(false);  // Removes the line next to the bars
        xAxis.setDrawLabels(false);    // Removes those decimal values (0,3, 0,6...)
        chart.invalidate(); // refresh

    }

}
