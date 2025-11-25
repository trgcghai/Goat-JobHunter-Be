package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum BlogActionType {
    DELETE("delete"), REJECT("reject");

    private final String value;

    BlogActionType(String value) {
        this.value = value;
    }
}
