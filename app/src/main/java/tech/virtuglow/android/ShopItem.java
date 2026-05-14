package tech.virtuglow.android;

import android.util.Log;

public class ShopItem extends Makeover {
    public ShopItem(int id, String name, String deeparFileName, String previewImage, double price, double averageRating) {
        super(id, name, deeparFileName, previewImage, price, averageRating);
        Log.d("OBJECT_DATA", "Constructing item with rating: " + averageRating);
    }
}
