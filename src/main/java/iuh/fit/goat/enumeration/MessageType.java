package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum MessageType {
    TEXT("text"), IMAGE("image"),
    VOICE("voice"), FILE("file");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }
}
