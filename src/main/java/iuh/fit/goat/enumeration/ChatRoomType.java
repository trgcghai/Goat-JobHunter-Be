package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum ChatRoomType {
    DIRECT("Trực tiếp"), GROUP("Nhóm"),
    BROADCAST("1 hoặc vài người nhắn"), AI("Ai");

    private final String value;

    ChatRoomType(String value) {
        this.value = value;
    }
}
