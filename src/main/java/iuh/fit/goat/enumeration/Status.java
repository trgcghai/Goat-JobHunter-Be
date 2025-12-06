package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum Status {
    PENDING("pending"), ACCEPTED("accepted"), REJECTED("rejected");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    public static Status fromValue(String value) {
        for (Status s : Status.values()) {
            if (s.getValue().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status value: " + value);
    }
}
