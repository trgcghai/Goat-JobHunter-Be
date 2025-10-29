package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Nam"), FEMALE("Nữ"), OTHER("Khác");

    private final String value;

    Gender(String value) {
        this.value = value;
    }
}
