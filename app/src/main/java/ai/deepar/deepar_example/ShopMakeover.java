package ai.deepar.deepar_example;

import java.util.ArrayList;

public class ShopMakeover {
    public String itemName;
    public int imageResourceId;
    public double price;
    public float rating;
   //TODO: public ArrayList<String> filters;


    public ShopMakeover(String itemName, int imageResourceId, double price, float rating) {
        this.itemName = itemName;
        this.imageResourceId = imageResourceId;
        this.price = price;
        this.rating = rating;
    }
}
