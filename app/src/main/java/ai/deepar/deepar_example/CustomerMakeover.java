package ai.deepar.deepar_example;

// USED TO MAKEOVER NAME AND FILTER FILE NAME WHEN WORKING
public class CustomerMakeover {
    public String makeoverName;
    public String filterFileName; //using for dummy testing!!
    public int makeoverID;      // makeover.makeoverID — primary key

    public String imagePreview; // makeover.imagePreview — URL/path for the thumbnail
    public String deeparFile;   // makeover.deeparFile — filename passed to PreviewActivity

//CONSTRUCTOR TO BE USED FOR DUMMYTESTING
    public CustomerMakeover(String makeoverName, String filterFileName) {
        this.makeoverName = makeoverName;
        this.filterFileName = filterFileName;
    }

//CONSTRUCTOR TO BE USED WHEN IMPLEMENTING DATABASE
    public CustomerMakeover(String makeoverName, int makeoverID, String imagePreview, String deeparFile) {
        this.makeoverName = makeoverName;
        this.makeoverID = makeoverID;
        this.imagePreview = imagePreview;
        this.deeparFile = deeparFile;
    }
}
