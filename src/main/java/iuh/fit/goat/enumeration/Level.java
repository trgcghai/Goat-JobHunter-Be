package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum Level {
    FRESHER("Fresher"), JUNIOR("Junior"), SENIOR("Senior"),
    INTERN("Intern"), MIDDLE("Middle");

    private final String value;

    Level(String value) {
        this.value = value;
    }
}
