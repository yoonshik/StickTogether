package com.yoonshikhong.sticktogether;


import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class User {
	private double longitude, latitude;
	private String uniqueIdentifier;
	private Marker marker;
	private GoogleMap map;
	private MarkerOptions markerOptions;
	private ValueEventListener coordinateListener;
	private String name;

	private Firebase userRef;

	/**
	 * Queries database for a user with the given phone number. If empty, creates a user in the db.
	 *
	 * @param rootRef
	 * @param phoneNumber
	 * @return The User object representing the user on the db
	 */
	public static User createUserByPhoneNumber(Firebase rootRef, String phoneNumber) {
		Firebase usersRef = rootRef.child("users");

		User user = new User(phoneNumber);
		user.userRef = usersRef.child(phoneNumber);

		return user;
	}

	/**
	 * Creates a new member with the specified identifier
	 * @param uniqueIdentifier any string unique to this member (e.g. phone number, facebook id, etc)
	 */
	public User(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
		markerOptions = new MarkerOptions();
	}

	public void addCoordinateListener() {
		coordinateListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				DataSnapshot longitudeSnap = dataSnapshot.child("longitude");
				DataSnapshot latitudeSnap = dataSnapshot.child("latitude");
				if (longitudeSnap.exists())
					longitude = (double) longitudeSnap.getValue();
				if (latitudeSnap.exists())
					latitude = (double) latitudeSnap.getValue();
				updateMap();
			}

			@Override
			public void onCancelled(FirebaseError firebaseError) {

			}
		};
		userRef.addValueEventListener(coordinateListener);
	}

	public void addValueEventListener(ValueEventListener listener) {
		userRef.addValueEventListener(listener);
	}

	void updateMap() {
		if (marker != null) {
			//remove old marker
			marker.remove();
		}
		marker = map.addMarker(markerOptions.position(new LatLng(latitude, longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_person_pin_resized_32)).title(uniqueIdentifier));
	}

	/**
	 * Sets user's coordinates and pushes to Firebase
	 *
	 * @param longitude
	 * @param latitude
	 */
	public void writeCoordinates(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
		userRef.child("longitude").setValue(longitude);
		userRef.child("latitude").setValue(latitude);
	}

	/**
	 * Sets user's name and pushes to Firebase
	 * @param name
	 */
	public void writeName(String name) {
		this.name = name;
		userRef.child("name").setValue(name);
	}

	/**
	 * Marks the user as having joined the given group
	 * @param group
	 */
	void joinGroup(Group group) {
		userRef.child("groups").child(group.getUniqueIdentifier()).setValue("joined");
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public void setMap(GoogleMap map) {
		this.map = map;
	}

}
