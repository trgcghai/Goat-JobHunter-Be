package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum InterviewStatus {
    SCHEDULED("Đã lên lịch"), COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"), RESCHEDULED("Đã lên lịch lại");

    private final String value;

    InterviewStatus(String value) {
        this.value = value;
    }
}
