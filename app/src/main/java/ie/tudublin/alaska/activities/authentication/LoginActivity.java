package ie.tudublin.alaska.activities.authentication;

import androidx.appcompat.app.AppCompatActivity;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.activities.MainActivity;
import ie.tudublin.alaska.helper.Util;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOGIN = 1234;

    private FirebaseAuth mAuth;
    private Util util;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        util = new Util();

        // verify network connectivity
        if(util.isNetworkAvailable(getApplicationContext())) {
            // user login status
            if(mAuth.getCurrentUser() != null) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
            } else {
                firebaseAuthLogin();
            }
        }
    }

    /**
     * creates and launches a Firebase login intent with
     * third-party auth providers returned by getProvider()
     */
    private void firebaseAuthLogin() {
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(getProvider())
                .setLogo(R.drawable.logo_login)
                .setTheme(R.style.LoginTheme)
                .setIsSmartLockEnabled(false)
                .setTosAndPrivacyPolicyUrls(
                        "https://www.freeprivacypolicy.com/privacy/view/3ccea0b97b785480095259db0c31569d",
                        "https://www.freeprivacypolicy.com/privacy/view/3ccea0b97b785480095259db0c31569d")
                .build(), REQUEST_CODE_LOGIN);
    }

    /**
     * returns a list of third-party auth providers
     * sign-in methods implemented are Facebook, Google and Twitter
     */
    private List<AuthUI.IdpConfig> getProvider() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.FacebookBuilder().setPermissions(Arrays.asList("email")).build(),
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.TwitterBuilder().build());

        return providers;
    }

    /**
     * handles result of the login process initiated by
     * startActivityForResult()
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LOGIN) {
            if (resultCode == RESULT_OK) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
            } else {
                String action = getApplicationContext().getResources().getString(R.string.message_action_failure, "login");
                Toast.makeText(this, action, Toast.LENGTH_SHORT).show();
            }
        }
    }
}