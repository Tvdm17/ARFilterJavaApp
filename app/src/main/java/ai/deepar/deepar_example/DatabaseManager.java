package ai.deepar.deepar_example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.content.Context;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// you cannot run network operations in the main thread, that's why we need to process this in the background
//



public class DatabaseManager {

    private static final String BASE_URL = "https://studev.groept.be/api/a25pt305/";

    public static final String PREVIEW_URL = "https://a25pt305.studev.groept.be/assets/previews/";

    public static final String EFFECTS_URL = "https://a25pt305.studev.groept.be/assets/effects/";

    private static final OkHttpClient client = createUnsafeOkHttpClient();
    private static int userid = -1;

    private static String username = "";

    private static String email = "";

    private static boolean isCustomer = true;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static List<Makeover> ownedMakeovers   = new ArrayList<>();
    public static List<ShopItem> shopItems        = new ArrayList<>();
    public static List<Makeover> creatorMakeovers = new ArrayList<>();




    public interface LoginCallback {
        void onSuccess(String email, int userId);
        void onFailure(String message);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("hashing error", e);
        }
    }

    private static OkHttpClient createUnsafeOkHttpClient() {
        try {

            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[]{};}
                    }
                };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch(Exception e){
                return new OkHttpClient();
            }

    }

    public static JSONArray fetchFromAPI(String endpoint) {
        String url = BASE_URL + endpoint.replace(" ", "%20");
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {

            if (response.isSuccessful() && response.body() != null) {
                return new JSONArray(response.body().string());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return new JSONArray();
    }


    public static void attemptLoginAsync(String email, String password, LoginCallback callback) {
        executor.execute(() -> {
            try {
                String hashedPassword = hashPassword(password);
                System.out.println("DEBUG HASH: " + hashedPassword);
                // The endpoint must match your API's naming convention
                JSONArray response = fetchFromAPI("check_user/" + email + "/" + hashedPassword);

                if (response != null && response.length() > 0) {
                    JSONObject userObj = response.optJSONObject(0);
                    if (userObj != null) {
                        String userEmail = userObj.optString("emailAddress", null);
                        int userId = userObj.optInt("userid", -1);
                        String username = userObj.optString("fullName", "");
                        if(username.isEmpty() || username.equals("null")){
                            username = userEmail;
                        }

                        setUsername(username);
                        setUserid(userId);// set userid for session
                        setEmail(userEmail);

                        // success, back to the ui thread
                        mainHandler.post(() -> callback.onSuccess(userEmail, userId));
                        return;
                    }
                }
                // if we reach here, either response was empty or object was null
                mainHandler.post(() -> callback.onFailure("Invalid email or password."));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }
        });
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public static void postToAPI(String endpoint, SimpleCallback callback, String... args) {
        executor.execute(() -> {
            try {
                // This builds the parameters part: /val1/val2/val3
                StringBuilder params = new StringBuilder();
                for (String arg : args) {
                    // handle special characters like @, encode them
                    params.append("/").append(java.net.URLEncoder.encode(arg, "UTF-8"));
                }

                String fullPath = endpoint + params.toString();
                JSONArray response = fetchFromAPI(fullPath);

                // Studev returns [] for successful INSERTs
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("API Error: " + e.getMessage()));
            }
        });
    }

    public static void fetchFromAPI(String serviceName, final SimpleCallback callback) {
        String url = "https://a25pt305.studev.groept.be/api/a25pt305/" + serviceName;
        Request request = new Request.Builder().url(url).build();
        executor.execute(() -> {
            try (Response response = client.newCall(request).execute()){

                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onFailure("Action failed"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });

    }

    public interface APICallback {
        void onSuccess(JSONArray response);
        void onFailure(String message);
    }

    public static void fetchFromAPI(String serviceName, final APICallback callback) {
        String url = "https://a25pt305.studev.groept.be/api/a25pt305/" + serviceName;

        executor.execute(() -> {
            try {
                // Use your preferred networking logic here (e.g., Volley or OkHttp)
                // This is a conceptual example of the hand-off:
                JSONArray responseArray =  fetchFromAPI(serviceName);

                if(responseArray != null){
                    mainHandler.post(() -> callback.onSuccess(responseArray));
                } else {
                    mainHandler.post(()-> callback.onFailure("empty response"));
                }

            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public static void resetUsername(){userid = -1;}

    public static int getUserid() {
        return userid;
    }

    public static String getUsername(){
        return username;
    }

    public static String getEmail() { return email;}

    public static void setUserid(int id){
        userid = id;
    }

    public static void setUsername(String name){username = name;}

    public static void setEmail(String emailgot){ email = emailgot;}

    public static void fetchOwnedMakeovers(int clientNumber, APICallback callback) {
        executor.execute(() -> {
            try {

                String endpoint = "get_makeovers/" + clientNumber;
                JSONArray response = fetchFromAPI(endpoint);

                if (response != null) {
                    mainHandler.post(() -> callback.onSuccess(response));
                } else {
                    mainHandler.post(() -> callback.onFailure("no makeovers found."));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }
        });
    }

    public static void fetchShopItems(int clientNumber, APICallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "get_shop_makeovers/" + clientNumber;
                JSONArray response = fetchFromAPI(endpoint);

                if (response != null) {
                    mainHandler.post(() -> callback.onSuccess(response));
                } else {
                    mainHandler.post(() -> callback.onFailure("Shop is empty!"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }
        });
    }


    public interface FileCallback {
        void onLoaded(String localPath);
        void onError(String message);
    }

    public static void downloadEffect(Context context, String fileName, FileCallback callback) {

        String fileUrl = EFFECTS_URL + fileName;

        // store it in the internal files directory so its private
        File targetFile = new File(context.getFilesDir(), fileName);

        // skip download if we already have it
        if (targetFile.exists()) {
            callback.onLoaded(targetFile.getAbsolutePath());
            return;
        }

        executor.execute(() -> {
            Request request = new Request.Builder().url(fileUrl).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Failed to download file: " + response);

                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(targetFile)) {

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }

                    mainHandler.post(() -> callback.onLoaded(targetFile.getAbsolutePath()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("Download failed: " + e.getMessage()));
            }
        });
    }

    public static void addPurchase(int clientId, int makeoverId, SimpleCallback simpleCallback) {

        postToAPI("add_purchase", simpleCallback,
                String.valueOf(clientId),
                String.valueOf(makeoverId)
        );

    }

    public static Makeover getMakeoverById(int id) {

        Log.d("SYNC_CHECK", "Searching for ID: " + id);
        Log.d("SYNC_CHECK", "Shop items count: " + shopItems.size());
        Log.d("SYNC_CHECK", "Owned items count: " + ownedMakeovers.size());

        for (Makeover m : ownedMakeovers) {
            if (m.getId() == id) return m;
        }
        for (ShopItem s : shopItems) {
            if (s.getId() == id) return s;
        }
        for (Makeover m : creatorMakeovers) {
            if (m.getId() == id) return m;
        }
        return null;
    }

    // ── Reviews ──────────────────────────────────────────────────────────────
    // Endpoint: GET get_reviews/{makeoverId}
    // Response: [{reviewId, makeoverID, userid, authorName, rating, comment}]
    public static void fetchReviews(int makeoverId, APICallback callback) {
        executor.execute(() -> {
            try {
                JSONArray response = fetchFromAPI("get_reviews/" + makeoverId);
                mainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    // Endpoint: GET get_creator_reviews/{userId}
    // Response: [{reviewId, makeoverID, userid, authorName, rating, comment, maskName}]
    public static void fetchCreatorReviews(int userId, APICallback callback) {
        executor.execute(() -> {
            try {
                JSONArray response = fetchFromAPI("get_creator_reviews/" + userId);
                mainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    // Endpoint: POST add_review/{makeoverId}/{userId}/{rating}/{comment}
    public static void addReview(int makeoverId, int userId, float rating, String comment, SimpleCallback callback) {
        postToAPI("add_review", callback,
                String.valueOf(makeoverId),
                String.valueOf(userId),
                String.valueOf(rating),
                comment);
    }

    // ── Creator makeovers ─────────────────────────────────────────────────────
    // Endpoint: GET get_creator_makeovers/{userId}
    // Response: [{makeoverID, name, deeparFile, imagePreview, price, averageRating}]
    public static void fetchCreatorMakeovers(int userId, APICallback callback) {
        executor.execute(() -> {
            try {
                JSONArray response = fetchFromAPI("get_creator_makeovers/" + userId);
                mainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    // Endpoint: POST create_makeover/{userId}/{name}
    // Returns the new makeoverID in response (backend should return [{makeoverID: X}])
    public static void createMakeover(int userId, String name, SimpleCallback callback) {
        postToAPI("create_makeover", callback,
                String.valueOf(userId),
                name);
    }

    // Endpoint: POST update_makeover/{makeoverId}/{name}
    public static void updateMakeover(int makeoverId, String name, SimpleCallback callback) {
        postToAPI("update_makeover", callback,
                String.valueOf(makeoverId),
                name);
    }

    public static void fetchTagsForMakeover(int makeoverId, APICallback callback) {
        executor.execute(() -> {
            try {
                JSONArray response = fetchFromAPI("get_makeover_tags/" + makeoverId);
                mainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public static boolean isItemOwned(int id){
        for(Makeover m : ownedMakeovers){
            if(m.getId() == id){
                return true;
            }
        }
        return false;
    }

    public static boolean isIsCustomer() {
        return isCustomer;
    }

    public static void setIsCustomer(boolean var) {
        isCustomer = var;
    }

}
