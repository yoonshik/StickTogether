package com.yoonshikhong.sticktogether;


import android.provider.ContactsContract;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class User {
	private double longitude, latitude;
	private String uniqueIdentifier;
	private Marker marker;
	private GoogleMap map;
	private MarkerOptions markerOptions;

	private Firebase userRef;

	/**
	 * Adds a new user to the database
	 *
	 * @param rootRef
	 * @param phoneNumber
	 * @return
	 */
	public static User registerNewUserByPhoneNumber(Firebase rootRef, String phoneNumber) {
		Firebase usersRef = rootRef.child("users");

		User user = new User(phoneNumber);
		user.userRef = usersRef.push();
		String userKey = user.userRef.getKey();

		UserFirebaseContainer container = new UserFirebaseContainer();
		container.setIdentifier(user.uniqueIdentifier);
		user.userRef.setValue(container);

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
		ValueEventListener listener = new ValueEventListener() {
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
		userRef.addValueEventListener(listener);
	}

	void updateMap() {
		if (marker != null) {
			//remove old marker
			marker.remove();
		}
		marker = map.addMarker(markerOptions.position(new LatLng(latitude, longitude)));
	}

	/**
	 * Sets user's coordinates and pushes to Firebase
	 *
	 * @param longitude
	 * @param latitude
	 */
	public void writeCoordinates(double longitude, double latitude) {
		String userKey = userRef.getKey();
		this.longitude = longitude;
		this.latitude = latitude;
		userRef.child("longitude").setValue(longitude);
		userRef.child("latitude").setValue(latitude);
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public void setMap(GoogleMap map) {
		this.map = map;
	}

}
