package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum MediaType {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }
}