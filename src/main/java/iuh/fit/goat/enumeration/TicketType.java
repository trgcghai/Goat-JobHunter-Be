package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum TicketType {
    BLOG_REPORT("blog"), COMMENT_REPORT("comment");

    private final String value;

    TicketType(String value) {
        this.value = value;
    }
}
