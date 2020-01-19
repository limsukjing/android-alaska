package ie.tudublin.alaska.activities.profile;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import ie.tudublin.alaska.model.User;

public class ProfileViewModel extends ViewModel {

    private FirebaseUser currentUser;
    private MutableLiveData<User> userLiveData;

    public ProfileViewModel() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    /**
     * get methods to retrieve user data such as
     * email address, username, and photo url
     */
    public LiveData<User> getUserData() {
        if(currentUser != null) {
            userLiveData = new MutableLiveData<>();

            User user = new User();
            user.setEmail(currentUser.getEmail());
            user.setUsername(currentUser.getDisplayName());

            if(currentUser.getPhotoUrl() != null) {
                user.setPhotoURL(currentUser.getPhotoUrl().toString());
            }

            userLiveData.setValue(user);
        }

        return userLiveData;
    }
}
