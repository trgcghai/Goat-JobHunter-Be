package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum FriendRequestStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    CANCELED("canceled"),
    EXPIRED("expired");

    private final String value;

    FriendRequestStatus(String value) {
        this.value = value;
    }
}
