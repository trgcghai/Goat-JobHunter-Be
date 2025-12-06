package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum Education {
    COLLEGE("Cao đẳng"), UNIVERSITY("Đại học"), SCHOOL("THPT"), ENGINEER("Kỹ sư");

    private final String value;

    Education(String value) {
        this.value = value;
    }
}
