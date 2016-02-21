package com.yoonshikhong.sticktogether;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 200; // 1 minute

    private static final int PICK_CONTACTS = 1510;

    private static final int CONTACT_PICKER_RESULT = 1001;
    private static final String DEBUG_TAG = "Contact List";
    private static final int RESULT_OK = -1;


	private Firebase myFirebaseRef;
    private GoogleMap mMap;
    private static final String TAG = "MapsActivity";
    private MarkerOptions markerOptions = null;
    private GoogleApiClient mGoogleApiClient;
    private Location myLocation;
    private LatLng currentLatLng;
    private LocationListener locationListener;
    private LinkedHashMap<String, String> contacts;
	//firebase representation of the current user
	private User self;
	private Group currentGroup;

    LocationManager locationManager;
    private boolean isGPSEnabled, isNetworkEnabled, canGetLocation = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Firebase.setAndroidContext(this);

	    Firebase myFirebaseRef = new Firebase("https://sweltering-inferno-8609.firebaseio.com/");

	    Group testGroup = Group.createNewGroup(myFirebaseRef);
	    User testUser = User.createUserByPhoneNumber(myFirebaseRef, "800STANLEYSTEAMER");
	    testGroup.joinMember(testUser);
        Firebase.setAndroidContext(this);

	    myFirebaseRef = new Firebase("https://sweltering-inferno-8609.firebaseio.com/");

	    String myNumber = AppUtils.formatPhoneNumber(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number());
	    //TODO don't register self if already in database
	    self = User.createUserByPhoneNumber(myFirebaseRef, myNumber);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Activity myActivity = this;

        final ImageButton button = (ImageButton) findViewById(R.id.button_id);
        button.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
//                startActivityForResult(intent, PICK_CONTACT);
		        Intent intent = new Intent(myActivity, ContactListActivity.class);
		        startActivityForResult(intent, PICK_CONTACTS);
	        }
        });

        TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        myNumber = tm.getLine1Number();
        Log.i(TAG, myNumber);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        locationManager = (LocationManager) getSystemService(MapsActivity.LOCATION_SERVICE);
        locationListener = new MyLocationListener();


        markerOptions = new MarkerOptions().title("Me");
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        Log.i(TAG, "onActivityResult");
        switch (reqCode) {
            case (PICK_CONTACTS) :
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, data.toString());
                    Uri contactData = data.getData();
                    ArrayList<String> names = data.getStringArrayListExtra("names");
                    ArrayList<String> numbers = data.getStringArrayListExtra("numbers");
	                currentGroup = Group.createNewGroup(myFirebaseRef);

                    for (int i = 0; i < names.size(); i++) {
	                    final User friend = User.createUserByPhoneNumber(myFirebaseRef,
			                    AppUtils.formatPhoneNumber(numbers.get(i)));
	                    friend.setMap(mMap);
	                    friend.addCoordinateListener();

	                    currentGroup.joinMember(friend);
                    }
                }
                break;
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            myLocation = LocationServices.FusedLocationApi.getLastLocation(
//                    mGoogleApiClient);
//        }

        myLocation = getLocation();

        if (myLocation==null) {
            Log.i(TAG, "Location unavailable");
        } else {
            currentLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
        }
    }



    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public Location getLocation() {
        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            locationManager = (LocationManager) this
                    .getSystemService(MapsActivity.LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Log.v("isGPSEnabled", "=" + isGPSEnabled);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Log.v("isNetworkEnabled", "=" + isNetworkEnabled);

            if (isGPSEnabled == false && isNetworkEnabled == false) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        myLocation = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (myLocation == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            myLocation = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return myLocation;
    }


    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            myLocation = getLocation();

			self.writeCoordinates(myLocation.getLongitude(), myLocation.getLatitude());

//            Toast.makeText(
//                    getBaseContext(),
//                    "Location changed: Lat: " + myLocation.getLatitude() + " Lng: "
//                            + myLocation.getLongitude(), Toast.LENGTH_SHORT).show();

        /*------- To get city name from coordinates -------- */
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(myLocation.getLatitude(),
                        myLocation.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}
