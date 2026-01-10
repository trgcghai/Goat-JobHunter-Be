package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum InterviewStatus {
    SCHEDULED("Đã lên lịch", "SCHEDULED"), COMPLETED("Hoàn thành", "COMPLETED"),
    CANCELLED("Đã hủy", "CANCELLED"), RESCHEDULED("Đã lên lịch lại", "RESCHEDULED");

    private final String value;
    private final String status;

    InterviewStatus(String value, String status) {
        this.value = value;
        this.status = status;
    }
}
