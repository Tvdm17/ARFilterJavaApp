package ai.deepar.deepar_example;


// CLASS USED TO ADAPT MAKEOVER FILTERS TO BE USED BY
// RECYCLEVIEW - into card views that RecycleView
// can display


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class MakeoverAdapter extends RecyclerView.Adapter<MakeoverAdapter.ViewHolder> {
    private final Context context;
    private final List<Makeover> makeovers;

    public MakeoverAdapter(Context context, List<Makeover> makeovers) {
        this.context = context;
        this.makeovers = makeovers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_owned, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Makeover makeover = makeovers.get(position);

        // Set the name label
        holder.tvItemName.setText(makeover.makeoverName);

        // When tapped, open MainActivity with this makeover's effect
        holder.ivItemImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("EFFECT_NAME", makeover.filterFileName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return makeovers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
        }
    }
}
