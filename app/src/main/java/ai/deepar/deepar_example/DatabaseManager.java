package ai.deepar.deepar_example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// you cannot run network operations in the main thread, that's why we need to process this in the background
//



public class DatabaseManager {

    private static final String BASE_URL = "https://studev.groept.be/api/a25pt305/";

    private static final OkHttpClient client = createUnsafeOkHttpClient();
    private static String username = "";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onComplete(JSONArray result);
    }


    public static void getHRDataAsync(String date, ApiCallback callback) {
        executor.execute(() -> {
            // This runs in the background
            JSONArray result = getHRData(date);

            // This sends the result back to the Main (UI) Thread
            mainHandler.post(() -> callback.onComplete(result));
        });
    }

    private static String hashPassword(String password) {
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

    private static JSONArray fetchFromAPI(String endpoint) {
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

    public static String attemptLogin(String usernameToLogin, String password) {
        String hashedPassword = hashPassword(password);
        JSONArray response = fetchFromAPI("check_user/" + usernameToLogin + "/" + hashedPassword);

        if (response != null && response.length() > 0) {
            try {
                // This line is what triggers the "Unhandled exception"
                username = response.getJSONObject(0).optString("username", null);
                return username;
            } catch (org.json.JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static boolean registerUser(String username, String password) {
        JSONArray check = fetchFromAPI("check_username/" + username);
        if (check != null && check.length() > 0) return false;
        String hashedPassword = hashPassword(password);
        fetchFromAPI("create_user/" + username + "/" + hashedPassword);
        return true;
    }

    public static void resetUsername(){username = "";}

    public static String getUsername() {
        return username;
    }

    // dynamic parameters restored for heart rate logic
    public static JSONArray getHRData(String date) {
        if(username.equals("admin")) {
            return fetchFromAPI("get_hr_data_admin/" + date);
        }
        return fetchFromAPI("get_hr_data_by_Name/" + username + "/" + date);
    }

    public static JSONArray getDHT11Data(int limit, String date) {
        if(username.equals("admin")) {

            return fetchFromAPI("get_dht11_get_dht11_data_admin/" + date);
        }
        return fetchFromAPI("get_dht11_data_by_Name/" + username + "/" + date);
    }

    public static JSONArray getNoiseData(int minute) {
        if(username.equals("admin")) {
            return fetchFromAPI("get_noise_data/" + minute);
        }
        return fetchFromAPI("get_noise_data_by_Name/" + username + "/" + minute);
    }

    public static JSONArray getBodyTempData(String date) {
        if(username.equals("admin")) {
            return fetchFromAPI("get_Body_Temp_admin/" + date);
        }
        return fetchFromAPI("get_Body_Temp_by_Name/" + username + "/" + date);
    }

    public static JSONArray getLiveDHT11Data(int minute) {
        if(username.equals("admin")) {

            return fetchFromAPI("get_live_dht11_data_admin/" + minute);
        }
        return fetchFromAPI("get_live_dht11_data_by_name/" + username + "/" + minute);
    }

    public static JSONArray getLiveBodyTempData(int minute) {
        if(username.equals("admin")) {
            return fetchFromAPI("get_live_Body_Temp_admin/" + minute);
        }
        return fetchFromAPI("get_live_Body_Temp_by_Name/" + username + "/" + minute);
    }

    public static JSONArray getLiveHRData(int minute) {
        if(username.equals("admin")) {
            return fetchFromAPI("get_live_hr_data_admin/" + minute);
        }
        return fetchFromAPI("get_live_hr_data_by_Name/" + username + "/" + minute);
    }

    public static JSONArray getStepData()
    {
        return fetchFromAPI("get_ACC_data_by_Name/" + username);
    }
}