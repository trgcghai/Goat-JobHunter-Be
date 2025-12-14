package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum CompanySize {
    STARTUP("Khởi nghiệp"), SMALL("Nhỏ"),
    MEDIUM("Vừa"), LARGE("Lớn"), ENTERPRISE("Tập đoàn");

    private final String value;

    CompanySize(String value) {
        this.value = value;
    }
}
