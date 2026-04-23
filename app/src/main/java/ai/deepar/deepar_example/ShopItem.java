package ai.deepar.deepar_example;

/**
 * Data model for one row from the shop — maps to the `makeover` table joined with `review`.
 *
 * DB query that produces this object:
 *   SELECT m.makeoverID, m.name, m.imagePreview, m.deeparFile, m.price,
 *          COALESCE(AVG(r.score), 0) AS averageRating
 *   FROM makeover m
 *   LEFT JOIN review r ON m.makeoverID = r.makeoverID
 *   GROUP BY m.makeoverID
 */
public class ShopItem {
    public int makeoverID;      // makeover.makeoverID — primary key
    public String name;         // makeover.name — display label
    public String imagePreview; // makeover.imagePreview — URL/path for the thumbnail
    public String deeparFile;   // makeover.deeparFile — filename passed to PreviewActivity
    public double averageRating;// AVG(review.score) for this makeover
    public double price;        // makeover.price

    public ShopItem(int makeoverID, String name, String imagePreview, String deeparFile, double averageRating, double price) {
        this.makeoverID = makeoverID;
        this.name = name;
        this.imagePreview = imagePreview;
        this.deeparFile = deeparFile;
        this.averageRating = averageRating;
        this.price = price;
    }
}
