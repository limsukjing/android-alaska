package ie.tudublin.alaska.helper;

import android.util.Base64;

import com.google.gson.Gson;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class TwitterAuth {
    private String CONSUMER_KEY;
    private String CONSUMER_SECRET;
    private final static String TOKEN_URL = "https://api.twitter.com/oauth2/token";
    private final static String STREAM_URL = "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=";

    public TwitterAuth(String consumerKey, String consumerSecret){
        this.CONSUMER_KEY = consumerKey;
        this.CONSUMER_SECRET = consumerSecret;
    }

    /**
     * encodes Twitter consumer key & secret
     */
    public String encodeToken(String screenName) {
        String res = null;
        
        try {
            String urlApiKey = URLEncoder.encode(CONSUMER_KEY, "UTF-8");
            String urlApiSecret = URLEncoder.encode(CONSUMER_SECRET, "UTF-8");
            String twitterKeySecret = urlApiKey + ":" + urlApiSecret;
            String twitterKeyBase64 = Base64.encodeToString(twitterKeySecret.getBytes(), Base64.NO_WRAP);
            TwitterAuthToken twitterAuthToken = getTwitterAuthToken(twitterKeyBase64);
            res = downloadTweets(screenName, twitterAuthToken);
        } catch (UnsupportedEncodingException | IllegalStateException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    /**
     * generates a bearer token
     */
    private TwitterAuthToken getTwitterAuthToken(String twitterKeyBase64) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(TOKEN_URL);
        httpPost.setHeader("Authorization", "Basic " + twitterKeyBase64);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
        Util httpUtil = new Util();
        String twitterJsonResponse = httpUtil.getHttpResponse(httpPost);

        return convertJsonToTwitterAuthToken(twitterJsonResponse);
    }

    private TwitterAuthToken convertJsonToTwitterAuthToken(String jsonAuth) {
        TwitterAuthToken twitterAuthToken = null;

        if (jsonAuth != null && jsonAuth.length() > 0) {
            try {
                Gson gson = new Gson();
                twitterAuthToken = gson.fromJson(jsonAuth, TwitterAuthToken.class);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        return twitterAuthToken;
    }

    private String downloadTweets(String screenName, TwitterAuthToken twitterAuthToken) {
        String res = null;

        if (twitterAuthToken != null && twitterAuthToken.token_type.equals("bearer")) {
            HttpGet httpGet = new HttpGet(STREAM_URL + screenName);
            httpGet.setHeader("Authorization", "Bearer " + twitterAuthToken.access_token);
            httpGet.setHeader("Content-Type", "application/json");
            Util httpUtil = new Util();
            res = httpUtil.getHttpResponse(httpGet);
        }

        return res;
    }

    private class TwitterAuthToken {
        String token_type;
        String access_token;
    }
}
