package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum ActionType {
    DELETE("delete"), REJECT("reject"), ACCEPT("accept"),
    ENABLE("enable"), DISABLE("disable");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }
}
