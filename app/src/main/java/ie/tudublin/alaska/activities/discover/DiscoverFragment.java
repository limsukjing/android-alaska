package ie.tudublin.alaska.activities.discover;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.adapter.DiscoverAdapter;

public class DiscoverFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    private ViewPager viewPager;
    private DiscoverAdapter mAdapter;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_discover, container, false);
        DiscoverViewModel discoverViewModel = new ViewModelProvider(this).get(DiscoverViewModel.class);

        mAdapter = new DiscoverAdapter(getContext());

        // Retrieve view objects
        viewPager = root.findViewById(R.id.discover_view_pager);

        getLocationPermission();

        return root;
    }

    /**
     * Request location permission to retrieve the location of the device
     * The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private void getLocationPermission() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            viewPager.setAdapter(mAdapter);
        }
    }

    /**
     * Callback for requestPermissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    viewPager.setAdapter(mAdapter);
                }
            } else if ((!ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()), Manifest.permission.ACCESS_FINE_LOCATION))) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.prompt_location)
                        .setMessage(R.string.prompt_location_permission)
                        .setPositiveButton(R.string.action_understand, (dialogInterface, i) -> {
                            Intent settingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            settingIntent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                            startActivity(settingIntent);
                        })
                        .create()
                        .show();
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.prompt_location)
                        .setMessage(R.string.prompt_location_permission)
                        .setPositiveButton(R.string.action_understand, (dialogInterface, i) -> getLocationPermission())
                        .create()
                        .show();
            }
        }
    }
}