package com.yoonshikhong.sticktogether;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MapsActivity";

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 200; // 1 minute

    private static final int PICK_CONTACTS = 1510;

    private Firebase myFirebaseRef;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location myLocation;
    private LocationListener locationListener;

    private Marker waypointMarker;
    private MarkerOptions waypointOptions;
    private double wpLongitude, wpLatitude;
    private boolean activeWaypoint = false;
    private CurrentGroupListener currentGroupListener;
    //firebase representation of the current user
    private User self;
    private Group currentGroup;


    LocationManager locationManager;

    /**
     * Listens for the self to be added to a group from another device
     */
    private class SelfListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (currentGroup == null && dataSnapshot.child("groups").exists()) { //must have children
                String firstGroupId = dataSnapshot.child("groups").getChildren().iterator().next().getKey();
                Group firstGroup = Group.getExistingGroup(myFirebaseRef, firstGroupId);
                setCurrentGroup(firstGroup);
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    private class CurrentGroupListener implements ValueEventListener {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            DataSnapshot waypointSnap = dataSnapshot.child("waypoint");
            if (waypointSnap.child("latitude").exists() && waypointSnap.child("longitude").exists()) {
                wpLatitude = (double) waypointSnap.child("latitude").getValue();
                wpLongitude = (double) waypointSnap.child("longitude").getValue();
                activeWaypoint = true;
                updateWaypoint();
            } else if (activeWaypoint) {
                activeWaypoint = false;
                updateWaypoint();
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Firebase.setAndroidContext(this);

        myFirebaseRef = new Firebase("https://sweltering-inferno-8609.firebaseio.com/");

        String myNumber = AppUtils.formatPhoneNumber(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number());
        self = User.createUserByPhoneNumber(myFirebaseRef, myNumber);
        self.addValueEventListener(new SelfListener());
        //set user name
        self.writeName(AppUtils.formatName(getUserName()));

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        waypointOptions = new MarkerOptions();

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

        final Button groupMessageButton = (Button) findViewById(R.id.group_message_button);
        groupMessageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Send tex");
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.putExtra("sms_body", "default content");
                sendIntent.putExtra("address", new String("3019560921;3017893695"));
                sendIntent.setType("vnd.android-dir/mms-sms");
                startActivity(sendIntent);
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
    }

    /**
     * Called when the self joins a group
     * @param group
     */
    private void setCurrentGroup(Group group) {
        if (currentGroup != null && currentGroupListener != null) {
            currentGroup.removeValueEventListener(currentGroupListener);
        }
        currentGroup = group;
        currentGroupListener = new CurrentGroupListener();
        group.addValueEventListener(currentGroupListener);
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

    private void updateWaypoint() {
        if (waypointMarker != null) {
            waypointMarker.remove();
            waypointMarker = null;
        }
        if (!activeWaypoint)
            return;

        waypointMarker = mMap.addMarker(waypointOptions.position(new LatLng(wpLatitude, wpLongitude)));
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        Log.i(TAG, "onActivityResult");
        switch (reqCode) {
            case (PICK_CONTACTS) :
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, data.toString());
                    ArrayList<String> names = data.getStringArrayListExtra("names");
                    ArrayList<String> numbers = data.getStringArrayListExtra("numbers");
                    Group currentGroup = Group.createNewGroup(myFirebaseRef);
                    currentGroup.joinMember(self);
                    for (int i = 0; i < names.size(); i++) {
                        final User friend = User.createUserByPhoneNumber(myFirebaseRef,
                                AppUtils.formatPhoneNumber(numbers.get(i)));
                        friend.writeName(AppUtils.formatName(names.get(i)));
                        friend.setMap(mMap);
                        friend.addCoordinateListener();

                        currentGroup.joinMember(friend);
                    }
                }
                break;
        }
    }

    public void setWaypointDialogue(String str) {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                MapsActivity.this);

        // Setting Dialog Title
        alertDialog2.setTitle("Set waypoint");

        // Setting Dialog Message
        alertDialog2.setMessage("Would you like to navigate to this location?");


        // Setting Positive "Yes" Btn
        alertDialog2.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog
                        Toast.makeText(getApplicationContext(),
                                "You clicked on YES", Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        // Setting Negative "NO" Btn
        alertDialog2.setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog
                        Toast.makeText(getApplicationContext(),
                                "You clicked on NO", Toast.LENGTH_SHORT)
                                .show();
                        dialog.cancel();
                    }
                });

        // Showing Alert Dialog
        alertDialog2.show();
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
            LatLng currentLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
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
        boolean isGPSEnabled, isNetworkEnabled;
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

            if (isGPSEnabled || isNetworkEnabled) {
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

            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(myLocation.getLatitude(),
                        myLocation.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
//                    String cityName = addresses.get(0).getLocality();
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

    private String getUserName() {
        Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
        c.moveToFirst();
        String retVal = c.getString(c.getColumnIndex("display_name"));
        c.close();
        return retVal;
    }

}
