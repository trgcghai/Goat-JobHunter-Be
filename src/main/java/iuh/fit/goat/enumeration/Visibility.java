package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum Visibility {
    PUBLIC("Cong khai"), PRIVATE("Rieng tu");

    private final String value;

    Visibility(String value) {
        this.value = value;
    }
}
