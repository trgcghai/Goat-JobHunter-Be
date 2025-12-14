package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum ChatRole {
    OWNER("Chủ phòng"), MODERATOR("Hỗ trợ"),
    VIEWER("Người xem"), MEMBER("Người nhắn");

    private final String value;

    ChatRole(String value) {
        this.value = value;
    }
}
