package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum ChatRoomType {
    DIRECT("Trực tiếp"), GROUP("Nhóm");

    private final String value;

    ChatRoomType(String value) {
        this.value = value;
    }
}
