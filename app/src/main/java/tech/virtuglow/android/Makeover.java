package tech.virtuglow.android;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Makeover {
    private int id;
    private String name;
    private String deeparFileName;
    private String previewImage;
    private double price;
    public double averageRating;

    private List<String> tags = new ArrayList<>();
    private boolean tagsLoaded = false;

    public Makeover(int id, String name, String deeparFileName, String previewImage, double price, double averageRating) {
        this.id = id;
        this.name = name;
        this.deeparFileName = deeparFileName;
        this.previewImage = previewImage;
        this.price = price;
        this.averageRating = averageRating;
    }

    // old dummy test code break prevention protocol I'm calling it TCBPP
    public Makeover(String name, String deeparFileName) {
        this.name = name;
        this.deeparFileName = deeparFileName;
        this.id = 0;
        this.previewImage = "default.jpg";
        this.price = 0.0;
    }

    public static Makeover fromJson(JSONObject obj) throws Exception {
        String preview = obj.isNull("imagePreview") ? "default.jpg" : obj.optString("imagePreview", "default.jpg");
        return new Makeover(
                obj.getInt("makeoverID"),
                obj.getString("name"),
                obj.getString("deeparFile"),
                preview,
                obj.optDouble("price", 0.0),
                obj.optDouble("averageRating", 0.0)
        );
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDeeparFileName() { return deeparFileName; }
    public String getPreviewImage() { return previewImage; }
    public double getPrice() { return price; }
    public double getAverageRating() { return averageRating; }
    public List<String> getTags() { return tags; }
    public boolean isTagsLoaded() { return tagsLoaded; }

    public void setTags(List<String> tags) {
        this.tags = tags;
        this.tagsLoaded = true;
    }
}
