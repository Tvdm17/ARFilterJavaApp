package ai.deepar.deepar_example;

public class Makeover {
    private int id;
    private String name;
    private String deeparFileName;
    private String previewUrl;
    private double price;

    public Makeover(int id, String name, String deeparFileName, String previewUrl, double price) {
        this.id = id;
        this.name = name;
        this.deeparFileName = deeparFileName;
        this.previewUrl = previewUrl;
        this.price = price;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDeeparFileName() { return deeparFileName; }
    public String getPreviewUrl() { return previewUrl; }
    public double getPrice() { return price; }
}
