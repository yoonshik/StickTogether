package com.yoonshikhong.sticktogether;


import com.firebase.client.Firebase;
import com.firebase.client.Query;

import java.util.ArrayList;

public class Group {
	private String uniqueIdentifier;
	private ArrayList<User> invitedMembers = new ArrayList<>();
	private ArrayList<User> joinedMembers = new ArrayList<>();

	private Firebase groupRef;
	private Query queryRef;

	/**
	 * Factory function to form a new group
	 *
	 * @param rootRef a reference to the firebase root
	 * @return a Group object representing the group formed
	 */
	public static Group createNewGroup(Firebase rootRef) {
		Firebase groupsRef = rootRef.child("groups");

		Group group = new Group();
		group.groupRef = groupsRef.push();
		group.uniqueIdentifier = group.groupRef.getKey();


		GroupFirebaseContainer container = new GroupFirebaseContainer();
		group.groupRef.setValue(container);

		return group;
	}

	public void joinMember(User newUser) {
		if (isJoined(newUser))
			return;

		Firebase newInvite = groupRef.child("members").push();
		newInvite.child("id").setValue(newUser.getUniqueIdentifier());
	}

	/**
	 * Returns true iff the user has joined the group
	 * @param user
	 */
	public boolean isJoined(User user) {
		return joinedMembers.contains(user);
	}
}
