package com.yoonshikhong.sticktogether;


import com.firebase.client.Firebase;

public class User {
	private double longitude, latitude;
	private String uniqueIdentifier;

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
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

}
