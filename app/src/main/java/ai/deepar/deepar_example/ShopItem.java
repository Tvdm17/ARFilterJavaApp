package ai.deepar.deepar_example;

import java.io.Serializable;

public class ShopItem extends Makeover implements Serializable {

    public double averageRating;
    public ShopItem(int id, String name, String deeparFileName, String previewImage, double price, double averageRating) {
        super(id, name, deeparFileName, previewImage, price);
        this.averageRating = averageRating;
    }

    public double getAverageRating() {
        return averageRating;
    }
}
