package ai.deepar.deepar_example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ShopMakeoverAdapter extends RecyclerView.Adapter<ShopMakeoverAdapter.ViewHolder> {
    private final Context context;
    private final List<ShopMakeover> makeovers;

    public ShopMakeoverAdapter(Context context, List<ShopMakeover> makeovers) {
        this.context = context;
        this.makeovers = makeovers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shop, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShopMakeover makeover = makeovers.get(position);
        holder.tvItemName.setText(makeover.itemName);

        holder.btnView.setOnClickListener(v -> {
            // TODO: create ItemCardActivity and uncomment below
            // Intent intent = new Intent(context, ItemCardActivity.class);
            // context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return makeovers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName;
        Button btnView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
}
