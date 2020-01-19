package ie.tudublin.alaska.activities.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

import java.io.InputStream;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.activities.authentication.LoginActivity;
import ie.tudublin.alaska.model.User;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel = ViewModelProviders.of(this).get(ProfileViewModel.class);
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        // retrieves view objects
        final TextView emailTextView = root.findViewById(R.id.text_email);
        final TextView usernameTextView = root.findViewById(R.id.text_username);
        ImageButton settingButton = root.findViewById(R.id.button_settings);

        final ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = manager.getActiveNetworkInfo();

        profileViewModel.getUserData().observe(this, (@Nullable User user) -> {
            emailTextView.setText(user.getEmail());
            usernameTextView.setText(user.getUsername());

            if(info != null && info.isConnected()) {
                new DownloadImageAsyncTask().execute(user.getPhotoURL());
            } else {
                Toast.makeText(getActivity(), R.string.message_network_error, Toast.LENGTH_SHORT).show();
            }
        });

        settingButton.setOnClickListener((View view) -> createSettingsMenu(view));

        return root;
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
                Toast.makeText(getActivity(), R.string.message_unknown_error, Toast.LENGTH_SHORT).show();

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
                        Toast.makeText(getActivity(), R.string.message_login_error, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getActivity(), R.string.message_login_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * creates a new thread to handle the user's profile picture download task
     */
    private class DownloadImageAsyncTask extends AsyncTask<String, Void, Drawable> {
        @Override
        protected Drawable doInBackground(String... urls) {
            try {
                return getImage(urls[0]);
            } catch(Exception e) {
                return null;
            }
        }

        // sets Drawable to Image View
        protected void onPostExecute(Drawable drawable) {
            if(drawable != null) {
                ImageView avatarImageView = getActivity().findViewById(R.id.image_avatar);

                if(avatarImageView != null) {
                    avatarImageView.setImageDrawable(drawable);
                }
            } else {
                Toast.makeText(getActivity(), R.string.message_avatar_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * retrieves user's profile picture as a Drawable object
     */
    private Drawable getImage(String imageURL) {
        try {
            InputStream inputStream = (InputStream) new URL(imageURL).getContent();
            return Drawable.createFromStream(inputStream, null);
        } catch(Exception e) {
            return null;
        }
    }
}