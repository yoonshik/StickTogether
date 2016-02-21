package com.yoonshikhong.sticktogether;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Admin on 2/20/2016.
 */
public class GroupFirebaseContainer {
	private Map<String, String> invites = new HashMap<>();
	private Map<String, String> members = new HashMap<>();

	public GroupFirebaseContainer() {}

	public Map<String, String> getMembers() {
		return members;
	}

	public void setMembers(Map<String, String> members) {
		this.members = members;
	}

	public Map<String, String> getInvites() {
		return invites;
	}

	public void setInvites(Map<String, String> invites) {
		this.invites = invites;
	}

}
