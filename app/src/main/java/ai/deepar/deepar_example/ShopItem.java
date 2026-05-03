package ai.deepar.deepar_example;

public class ShopItem extends Makeover{

    public double averageRating;
    public ShopItem(int id, String name, String deeparFileName, String previewImage, double price, double averageRating) {
        super(id, name, deeparFileName, previewImage, price);
        this.averageRating = averageRating;
    }
}
