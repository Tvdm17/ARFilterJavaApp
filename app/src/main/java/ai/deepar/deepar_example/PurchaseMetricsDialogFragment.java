package ai.deepar.deepar_example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.charts.LineChart;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class PurchaseMetricsDialogFragment extends DialogFragment {

    private static final String ARG_MAKEOVER_ID  = "makeover_id";
    private static final String ARG_AVG_RATING   = "avg_rating";

    private static int makeoverID = -1;

    public static PurchaseMetricsDialogFragment newInstance(int makeoverId, float avgRating) {

        makeoverID = makeoverId;

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

        initDataAdds(view.findViewById(R.id.chartAdds));
        initDataRemoves(view.findViewById(R.id.chartRemoves));
        initEmptyChart(view.findViewById(R.id.chartReviews));

        view.findViewById(R.id.btnCloseMetrics).setOnClickListener(v -> dismiss());
    }

    private void initEmptyChart(LineChart chart) {
        chart.setNoDataText("Data coming soon");
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }
    private void initDataAdds(LineChart chart){

        DatabaseManager.fetchRemoves(String.valueOf(makeoverID) ,new DatabaseManager.APICallback() {

            @Override
            public void onSuccess(JSONArray response) {

                //chart.setData();

            }

            @Override
            public void onFailure(String message) {

            }
        });
        //chart.setData("");
    }
    private void initDataRemoves(LineChart chart){

        DatabaseManager.fetchRemoves(String.valueOf(makeoverID) ,new DatabaseManager.APICallback() {

            @Override
            public void onSuccess(JSONArray response) {



            }

            @Override
            public void onFailure(String message) {

            }
        });
        //chart.setData("");
    }
}
