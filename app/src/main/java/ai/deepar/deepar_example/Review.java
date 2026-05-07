package ai.deepar.deepar_example;

import org.json.JSONObject;

public class Review {

    private final int reviewId;
    private final int makeoverId;
    private final int userId;
    private final String authorName;
    private final float rating;
    private final String comment;

    public Review(int reviewId, int makeoverId, int userId, String authorName, float rating, String comment) {
        this.reviewId   = reviewId;
        this.makeoverId = makeoverId;
        this.userId     = userId;
        this.authorName = authorName;
        this.rating     = rating;
        this.comment    = comment;
    }

    public static Review fromJson(JSONObject obj) {
        return new Review(
                obj.optInt("reviewId", 0),
                obj.optInt("makeoverID", 0),
                obj.optInt("userid", 0),
                obj.optString("authorName", "Unknown"),
                (float) obj.optDouble("rating", 0.0),
                obj.optString("comment", "")
        );
    }

    public int getReviewId()    { return reviewId; }
    public int getMakeoverId()  { return makeoverId; }
    public int getUserId()      { return userId; }
    public String getAuthorName() { return authorName; }
    public float getRating()    { return rating; }
    public String getComment()  { return comment; }
}
