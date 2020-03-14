package ie.tudublin.alaska.helper;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ie.tudublin.alaska.R;

public class PlacesParser {
    private Context mContext;

    public PlacesParser(Context context) {
        mContext = context;
    }

    /**
     * Parse results returned by Google Places API
     */
    public List<HashMap<String, String>> parse(String jsonData) {
        JSONArray jsonArray = null;
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(jsonData);
            jsonArray = jsonObject.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assert jsonArray != null;
        return getPlaces(jsonArray);
    }

    /**
     * Add results to a List
     */
    private List<HashMap<String, String>> getPlaces(JSONArray jsonArray) {
        List<HashMap<String, String>> placesList = new ArrayList<>();
        HashMap<String, String> place;

        if (jsonArray.length() > 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    place = getPlace((JSONObject) jsonArray.get(i));
                    placesList.add(place);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            String message = mContext.getString(R.string.message_hospital_empty) + " " + mContext.getString(R.string.message_location_empty);
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }

        return placesList;
    }


    /**
     * Get individual place details
     */
    private HashMap<String, String> getPlace(JSONObject placeJson) {
        HashMap<String, String> placeMap = new HashMap<>();

        try {
            if (!placeJson.isNull("name")) {
                placeMap.put("name", placeJson.getString("name"));
            }
            if (!placeJson.isNull("vicinity")) {
                placeMap.put("vicinity", placeJson.getString("vicinity"));
            }

            placeMap.put("latitude", placeJson.getJSONObject("geometry").getJSONObject("location").getString("lat"));
            placeMap.put("longitude", placeJson.getJSONObject("geometry").getJSONObject("location").getString("lng"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return placeMap;
    }
}
