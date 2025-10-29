package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum InterviewType {
    PERSON("Trực tiếp"), VIDEO("Video"), PHONE("Điện thoại"), TECHNICAL("Kỹ thuật");

    private final String value;

    InterviewType(String value) {
        this.value = value;
    }
}
