package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum ReportReason {
    SPAM("Spam"), HARASSMENT("Quấy rối"), HATE_SPEECH("Lời nói bậy"),
    VIOLENCE("Bạo lực"), FALSE_INFO("Tin giả"), OTHER("Lý do khác");

    private final String value;

    ReportReason(String value) {
        this.value = value;
    }
}
