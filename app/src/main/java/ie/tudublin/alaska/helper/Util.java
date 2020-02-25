package ie.tudublin.alaska.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import id.zelory.compressor.Compressor;
import ie.tudublin.alaska.R;

public class Util {
    /**
     * Use ConnectivityManager to check network connection
     */
    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected) {
            return true;
        } else {
            String action = context.getResources().getString(R.string.message_error, "Network error");
            Toast.makeText(context, action, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Source: https://github.com/zetbaitsu/Compressor
     * Use Compression Library to compress the images before uploading to Firestore
     */
    public byte[] compressImage(Uri imageUri, Context context, int maxHeight, int maxWidth, int quality) {
        File imageFile = new File(imageUri.getPath());
        Bitmap compressedImg = null;

        try {
            compressedImg = new Compressor(context)
                    .setMaxHeight(maxHeight)
                    .setMaxWidth(maxWidth)
                    .setQuality(quality)
                    .compressToBitmap(imageFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        compressedImg.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Retrieve the body of a HTTP Request
     */
    public String getHttpResponse(HttpRequestBase request) {
        String res = null;

        try {
            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            HttpResponse httpResponse = httpClient.execute(request);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String reason = httpResponse.getStatusLine().getReasonPhrase();
            StringBuilder sb = new StringBuilder();

            if (statusCode == 200) {
                HttpEntity entity = httpResponse.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                String line;
                while ((line = bReader.readLine()) != null) sb.append(line);
            } else sb.append(reason);

            res = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }
}
