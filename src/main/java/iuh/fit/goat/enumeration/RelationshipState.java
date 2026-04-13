package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum RelationshipState {
    FRIEND("friend"),
    BLOCKED("blocked");

    private final String value;

    RelationshipState(String value) {
        this.value = value;
    }
}
