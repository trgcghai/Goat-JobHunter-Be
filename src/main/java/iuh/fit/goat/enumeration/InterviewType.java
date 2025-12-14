package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum InterviewType {
    PERSON("Trực tiếp"),
    VIDEO("Video"),
    TECHNICAL("Kỹ thuật"),
    ONLINE_TEST("Bài test online"),
    HR_INTERVIEW("Phỏng vấn HR"),
    GROUP_INTERVIEW("Phỏng vấn nhóm"),
    PANEL_INTERVIEW("Phỏng vấn hội đồng"),
    ASSESSMENT_CENTER("Trung tâm đánh giá");

    private final String value;

    InterviewType(String value) {
        this.value = value;
    }
}
