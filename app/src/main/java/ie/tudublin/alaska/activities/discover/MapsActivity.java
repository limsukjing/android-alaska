package ie.tudublin.alaska.activities.discover;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.helper.PlacesParser;
import ie.tudublin.alaska.helper.Util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMyLocationButtonClickListener {

    private Util util;

    private GoogleMap mMap;
    private Location mLocation;
    private Marker mMarker;

    private String data;
    private LatLng mLatLng;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        util = new Util();

        // obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // retrieve view objects
        progressBar = findViewById(R.id.map_progress_bar);
    }


    /**
     * Manipulate the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String permissionDenied = getString(R.string.message_permission_denied,"access location");
            Toast.makeText(this, permissionDenied, Toast.LENGTH_SHORT).show();
            return;
        }

        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);

        getLastLocation();
    }


    /**
     * Use FusedLocationProviderClient to retrieve the device's last known location.
     * Add marker to Google Map on click.
     */
    public void getLastLocation() {
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Task<Location> locationTask = mFusedLocationProviderClient.getLastLocation();

        // This callback is triggered when user's location is retrieved
        locationTask.addOnSuccessListener(this, location -> {
            if (location != null) {
                mLocation = location;

                // Move camera to current location
                mLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                float zoom = 10f;
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mLatLng, zoom);
                mMap.animateCamera(cameraUpdate);

               displayMarker(mLatLng);
            } else {
                String action = getString(R.string.message_action_failure,"retrieve last known location");
                Toast.makeText(this, action, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Add marker to Google Map on click.
     */
    @Override
    public void onMapClick(LatLng latLng) {
        mMap.clear(); // Clear previous markers
        mLatLng = latLng;
        displayMarker(mLatLng);
    }


    /**
     * Retrieve user's current location.
     */
    @Override
    public boolean onMyLocationButtonClick() {
        mMap.clear(); // Clear previous markers
        getLastLocation();
        return true;
    }


    /**
     * Display marker on given latitude and longitude values.
     */
    private void displayMarker(LatLng latLng) {
        String[] address = getAddress(latLng.latitude, latLng.longitude);
        mMarker = mMap.addMarker(new MarkerOptions().title(address[0]).snippet(address[1]).position(latLng));
        mMarker.showInfoWindow();

        mMap.setOnInfoWindowClickListener(marker -> locationPopupDialog(address).show());
    }


    /**
     * Get complete address from location's latitude and longitude.
     * Return an array of string: res[0] is the full address, whereas
     * res[1] is the country code
     */
    private String[] getAddress(double LATITUDE, double LONGITUDE) {
        String[] res = new String[2];

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);

            if(addressList.size() > 0) {
                res[0] = addressList.get(0).getAddressLine(0); // full address
                res[1] = addressList.get(0).getCountryName(); // country name
            } else {
                Toast.makeText(this, R.string.message_location_error, Toast.LENGTH_SHORT).show();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return res;
    }


    /**
     * Use the Builder class to construct an AlertDialog for location confirmation
     */
    private Dialog locationPopupDialog(final String[] address) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.title_location)
                .setMessage(address[0]) // full address
                .setPositiveButton(R.string.action_confirm, (dialog, id) -> {
                    if (mLatLng != null) {
                        String url = getPlacesUrl(mLatLng);
                        new PlacesAsyncTask().execute(url);
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, id) -> Toast.makeText(this, R.string.message_location_empty, Toast.LENGTH_SHORT).show())
                .setCancelable(false);

        // Create the AlertDialog object and return it
        return builder.create();
    }


    /**
     * Use the Builder class to construct an AlertDialog for navigation confirmation
     */
    private Dialog navigationPopupDialog(String name, String vicinity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.title_activity_maps)
                .setMessage(getString(R.string.prompt_navigation, name, vicinity)) // hospital's name
                .setPositiveButton(R.string.action_confirm, (dialog, id) -> {
                    if (name != null) {
                        String geoLocation = "google.navigation:q=" + name + " " + vicinity;
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(geoLocation));

                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, id) -> Toast.makeText(getApplicationContext(), R.string.message_location_empty, Toast.LENGTH_SHORT).show())
                .setCancelable(false);

        // Create the AlertDialog object and return it
        return builder.create();
    }


    /**
     * Build a Find Place request for places of type 'hospital' within a 2000m radius of user's current/custom location
     */
    private String getPlacesUrl(LatLng mLatLng) {
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + mLatLng.latitude + "," + mLatLng.longitude +
                "&radius=" + 2000 +
                "&type=" + "hospital" +
                "&sensor=true" +
                "&key=" + getString(R.string.google_maps_key);
    }


    /**
     * Connect to Google API and retrieve locations for nearby hospitals using an AsyncTask
     */
    private class PlacesAsyncTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String url = params[0];
                data = util.downloadUrl(url);
            } catch (Exception e) {
               e.printStackTrace();
            }

            return data;
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            List<HashMap<String, String>> placeList;

            if (res.length() > 0) {
                PlacesParser parser = new PlacesParser(getApplicationContext());
                placeList =  parser.parse(res);
                displayNearbyHospitals(placeList);

                progressBar.setVisibility(View.INVISIBLE);
            }
        }

        private void displayNearbyHospitals(List<HashMap<String, String>> placeList) {
            if (placeList.size() > 0) {
                for (int i = 0; i < placeList.size(); i++) {
                    HashMap<String, String> place = placeList.get(i);

                    double lat = Double.parseDouble(Objects.requireNonNull(place.get("latitude")));
                    double lng = Double.parseDouble(Objects.requireNonNull(place.get("longitude")));
                    String name = place.get("name");
                    String vicinity = place.get("vicinity");
                    LatLng latLng = new LatLng(lat, lng);

                    mMarker = mMap.addMarker(new MarkerOptions()
                            .title(name)
                            .snippet(vicinity)
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                    // Move camera to current location
                    float zoom = 12f;
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
                    mMap.animateCamera(cameraUpdate);

                    mMap.setOnInfoWindowClickListener(marker -> navigationPopupDialog(marker.getTitle(), marker.getSnippet()).show());
                }
            }
        }
    }
}