package ie.tudublin.alaska.activities.discover;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.helper.Util;

public class DiscoverFragment extends Fragment implements View.OnClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    private DiscoverViewModel discoverViewModel;
    private Util util;

    private ImageButton podcastButton;
    private ImageButton mapButton;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        discoverViewModel = ViewModelProviders.of(this).get(DiscoverViewModel.class);
        View root = inflater.inflate(R.layout.fragment_discover, container, false);

        util = new Util();

        // Retrieve view objects
        podcastButton = root.findViewById(R.id.btn_podcast);
        mapButton = root.findViewById(R.id.btn_map);

        getLocationPermission();

        return root;
    }

    /**
     * Request location permission to retrieve the location of the device
     * The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private void getLocationPermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            podcastButton.setOnClickListener(this);
            mapButton.setOnClickListener(this);
        }
    }

    /**
     * Callback for requestPermissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                podcastButton.setOnClickListener(this);
                mapButton.setOnClickListener(this);
            } else {
                getLocationPermission();
            }
        }
    }

    public void onClick(View view) {
        if(util.isNetworkAvailable(getContext())) {
            switch (view.getId()) {
                case R.id.btn_podcast:
                    Toast.makeText(getContext(), "Podcast Button", Toast.LENGTH_SHORT).show();
                    break;

                case R.id.btn_map:
                    Toast.makeText(getContext(), "Map Button", Toast.LENGTH_SHORT).show();
                    break;
            }
        } else {
            Toast.makeText(getContext(), R.string.message_network_error, Toast.LENGTH_SHORT).show();
        }
    }
}