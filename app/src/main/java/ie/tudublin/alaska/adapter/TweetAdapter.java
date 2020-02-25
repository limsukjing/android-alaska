package ie.tudublin.alaska.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.activities.profile.ProfileFragment;

public class TweetAdapter extends BaseAdapter {

    private FirebaseStorage mStorage;

    private Activity mActivity;
    private ArrayList<HashMap<String, String>> mData;
    private String tone, tweet, imgPath;

    public TweetAdapter(Activity activity, ArrayList<HashMap<String, String>> data) {
        mActivity = activity;
        mData = data;
    }

    // getter
    public int getCount() {
        return mData.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * Render custom tweet item
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        mStorage = FirebaseStorage.getInstance();
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mActivity).inflate(R.layout.tweet_item, parent, false); // custom list view

            holder.toneImgView = convertView.findViewById(R.id.tweet_mood_img);
            holder.moodTextView = convertView.findViewById(R.id.tweet_mood_text);
            holder.contentTextView = convertView.findViewById(R.id.tweet_content_text);
            convertView.setTag(holder); // set references to view objects
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.toneImgView.setId(position);
        holder.moodTextView.setId(position);
        holder.contentTextView.setId(position);

        HashMap<String, String> map;
        map = mData.get(position);

        // Update view objects
        try {
            tone = mActivity.getResources().getString(R.string.prompt_twitter_mood, formatData(map.get(ProfileFragment.KEY_MOOD)));
            tweet = map.get(ProfileFragment.KEY_TWEET);

            holder.moodTextView.setText(tone);
            holder.contentTextView.setText(tweet);

            if (imgPath != null) {
                mStorage
                        .getReference()
                        .child(imgPath)
                        .getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Picasso.get()
                                    .load(uri.toString())
                                    .resize(100, 100)
                                    .centerCrop()
                                    .placeholder(R.color.tw__light_gray)
                                    .into(holder.toneImgView);
                        })
                        .addOnFailureListener(Throwable::printStackTrace);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }

    private static class ViewHolder {
        CircleImageView toneImgView;
        TextView contentTextView, moodTextView;
    }

    /**
     * Deal with null values.
     */
    private String formatData(String data) {
        String[] tones = {"anger", "fear", "joy", "sadness", "analytical", "confident", "tentative"};

        if (data == null || data.isEmpty() || data.equals("null")) {
            return "N/A";
        } else if (!Arrays.asList(tones).contains(data)) {
            imgPath = mActivity.getResources().getString(R.string.title_tone_img_url, "neutral");
        } else {
            imgPath = mActivity.getResources().getString(R.string.title_tone_img_url, data);
        }

        return data.toUpperCase();
    }
}