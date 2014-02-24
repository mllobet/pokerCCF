package com.intel.friend.invitation;

public enum FriendInvitationMessage {

	AddFriendSucceeded(0), 
	InviteeIsBusy(1), 
	InvitationCancelledBySender(2), 
	InvitationNotAccepted(3), 
	InvitationAccepted(4),
	InvitationAcceptedButInternetNotAvailable(5), 
	AnErrorOccurred(6);
	
	private final int value;

	private FriendInvitationMessage(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}