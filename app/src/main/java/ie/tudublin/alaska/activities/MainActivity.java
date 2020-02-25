package ie.tudublin.alaska.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.activities.authentication.LoginActivity;
import ie.tudublin.alaska.helper.Util;

public class MainActivity extends AppCompatActivity {

    private Util util;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        util = new Util();

        // verify network connectivity
        if(util.isNetworkAvailable(getApplicationContext())) {
            setContentView(R.layout.activity_main);

            // checks the login status of a user
            // redirects user to login screen if user is not authenticated
            if(FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, R.string.message_authentication_error, Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
                return;
            }
        }

        // retrieves view objects
        BottomNavigationView navView = findViewById(R.id.nav_view);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);
    }
}
