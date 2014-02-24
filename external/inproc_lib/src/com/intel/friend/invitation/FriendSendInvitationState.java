package com.intel.friend.invitation;

public enum FriendSendInvitationState
{
    PreconfirmSendInvitation,
    NoLocalInternetConnection,
    InviteeBusy,
    ConnectingToInvitee,
    ConnectionFailed,
    InviteeNoInternetError,
    InvitationAccepted,
    ConnectedMakingFriendRequest,
    FriendRequestTimedOut,
    FriendRequestNotAccepted,
    ServerRegistrationFailed,
    ServerRegistrationSucceededFriendsMade,
    CloseDialog
}