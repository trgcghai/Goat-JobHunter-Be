package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum MessageRole {
    USER("User"), AI("Ai");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }
}
