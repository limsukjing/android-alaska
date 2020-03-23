package ie.tudublin.alaska.activities.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.activities.authentication.LoginActivity;
import ie.tudublin.alaska.adapter.TweetAdapter;
import ie.tudublin.alaska.helper.Analyzer;
import ie.tudublin.alaska.helper.TwitterAuth;
import ie.tudublin.alaska.helper.Util;
import ie.tudublin.alaska.model.User;

public class ProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;

    private TextView emailText, usernameText;
    private EditText usernameEditText;
    private ImageView avatarImage;
    private ImageButton settingsButton;
    private Button twitterButton, analyzeButton;
    private ProgressBar progressBar;
    private BottomSheetDialog dialog;
    private ListView listView;

    private Util util;
    private Analyzer analyzer;
    private String twitterScreenName;
    private ArrayList<String> tweetData = new ArrayList<>();
    private ArrayList<HashMap<String, String>> tweetList = new ArrayList<>();
    public static final String KEY_TWEET = "tweet";
    public static final String KEY_MOOD = "mood";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        util = new Util();

        // retrieves view objects
        emailText = root.findViewById(R.id.profile_email_text);
        usernameText = root.findViewById(R.id.profile_username_text);
        avatarImage = root.findViewById(R.id.profile_avatar);
        settingsButton = root.findViewById(R.id.profile_settings_btn);
        twitterButton = root.findViewById(R.id.profile_twitter_btn);

        // verify network connectivity
        if(util.isNetworkAvailable(getContext())) {
            profileViewModel.getUserData().observe(getViewLifecycleOwner(), (@Nullable User user) -> {
                emailText.setText(user.getEmail());
                usernameText.setText(user.getUsername());

                Picasso.get()
                        .load(user.getPhotoURL())
                        .resize(500, 500)
                        .centerCrop()
                        .placeholder(R.color.tw__light_gray)
                        .into(avatarImage);
            });
        }

        settingsButton.setOnClickListener(this::createSettingsMenu);
        twitterButton.setOnClickListener(this::createTwitterDialog);

        return root;
    }

    /**
     * retrieves user's Twitter handle using BottomSheetDialog
     */
    private void createTwitterDialog(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.twitter_bottom_sheet, null);
        dialog = new BottomSheetDialog(view.getContext());
        dialog.setContentView(dialogView);
        dialog.show();

        // retrieves view objects
        usernameEditText = dialogView.findViewById(R.id.twitter_username_edit);
        analyzeButton = dialogView.findViewById(R.id.twitter_analyze_btn);
        listView = dialogView.findViewById(R.id.twitter_list_view);
        progressBar = dialogView.findViewById(R.id.twitter_progress_bar);

        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                listView.setAdapter(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        analyzeButton.setOnClickListener(v -> {
            twitterScreenName = usernameEditText.getText().toString();

            if (twitterScreenName.length() > 0) {
                if (util.isNetworkAvailable(Objects.requireNonNull(getContext()))) {
                    new TwitterAsyncTask().execute(twitterScreenName);
                }
            } else {
                Toast.makeText(getContext(), R.string.message_input_empty, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * connects to Twitter and downloads user's timeline using an AsyncTask
     */
    private class TwitterAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            final String TWITTER_CONSUMER_KEY = getResources().getString(R.string.twitter_consumer_key);
            final String TWITTER_CONSUMER_SECRET = getResources().getString(R.string.twitter_consumer_secret);

            String res = null;

            if (params.length > 0) {
                TwitterAuth twitterAuth = new TwitterAuth(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);
                res = twitterAuth.encodeToken(params[0]);
            }

            return res;
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);

            // user exists
            if (res.length() > 100) {
                try {
                    JSONArray jsonArray = new JSONArray(res);
                    tweetData.clear();

                    if (jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++){
                            JSONObject jsonObj = jsonArray.getJSONObject(i);
                            String tweet = jsonObj.getString("text");
                            tweetData.add(tweet);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (res.equals("[]")) {
                Toast.makeText(getContext(), getString(R.string.message_tweet_empty), Toast.LENGTH_SHORT).show(); // user exists but has 0 tweet
            } else {
                Toast.makeText(getContext(), res, Toast.LENGTH_SHORT).show(); // private account or user does not exist
            }

            //noinspection unchecked
            new AnalyzerAsyncTask().execute(tweetData);
        }
    }

    private class AnalyzerAsyncTask extends AsyncTask<ArrayList<String>, Void, ArrayList<HashMap<String, String>>> {
        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(ArrayList<String>... params) {
            tweetList.clear();

            for (int i = 0; i < params[0].size(); i++) {
                try {
                    analyzer = new Analyzer(getContext());
                    analyzer.setText(params[0].get(i));
                    String mood = analyzer.getToneResult();

                    // key-value pairs
                    HashMap<String, String> map = new HashMap<>();
                    map.put(KEY_TWEET, params[0].get(i));
                    map.put(KEY_MOOD, mood);
                    tweetList.add(map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return tweetList;
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> res) {
            super.onPostExecute(res);

            TweetAdapter tweetAdapter = new TweetAdapter(getActivity(), res);
            listView.setAdapter(tweetAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * creates a popup menu for profile settings that contain two options:
     * logout and delete user account
     * library used: https://github.com/skydoves/PowerMenu
     */
    private void createSettingsMenu(View view) {
         PowerMenu profileMenu = new PowerMenu.Builder(getContext())
                .addItem(new PowerMenuItem(getString(R.string.action_logout), false))
                .addItem(new PowerMenuItem(getString(R.string.action_delete_account), false))
                .setAnimation(MenuAnimation.ELASTIC_CENTER) // Animation start point (TOP | LEFT)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .setMenuColor(Color.WHITE)
                .setOnMenuItemClickListener(onMenuItemClick)
                .build();

        profileMenu.showAsDropDown(view);
    }

    private OnMenuItemClickListener<PowerMenuItem> onMenuItemClick = (int position, PowerMenuItem item) -> {
        switch(item.getTitle()) {
            case "Logout":
                firebaseAuthLogout();
                break;
            case "Delete Account":
                firebaseAuthDelete();
                break;
            default:
                String action = getContext().getResources().getString(R.string.message_error, "An error");
                Toast.makeText(getContext(), action, Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * handles result of the logout process initiated by Firebase Auth UI's signOut()
     */
    private void firebaseAuthLogout() {
        AuthUI.getInstance()
                .signOut(getActivity())
                .addOnCompleteListener((@NonNull Task<Void> task) -> {
                    if (task.isSuccessful()) {
                        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(loginIntent);
                    } else {
                        String action = getContext().getResources().getString(R.string.message_action_failure, "login");
                        Toast.makeText(getActivity(), action, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * handles result of the user account removal process initiated by
     * Firebase Auth UI's delete()
     */
    private void firebaseAuthDelete() {
        AuthUI.getInstance()
                .delete(getActivity())
                .addOnCompleteListener((@NonNull Task<Void> task) -> {
                    if (task.isSuccessful()) {
                        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(loginIntent);
                    } else {
                        String action = getContext().getResources().getString(R.string.message_action_failure, "login");
                        Toast.makeText(getActivity(), action, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}