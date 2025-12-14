package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum ReactionType {
    LIKE("Thích"), CELEBRATE("Chúc mừng"), SUPPORT("Ủng hộ"),
    LOVE("Yêu"), INSIGHTFUL("Sâu sắc"), FUNNY("Hài hước"),;

    private final String value;

    ReactionType(String value) {
        this.value = value;
    }
}
