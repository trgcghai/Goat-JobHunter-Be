package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum RatingType {

    OVERALL("Đánh giá chung", "overall"),
    SALARY("Lương thưởng và phúc lợi", "salary"),
    TRAINING("Đào tạo và học hỏi", "training"),
    MANAGEMENT("Sự quan tâm đến nhân viên", "management"),
    CULTURE("Văn hóa công ty", "culture"),
    OFFICE("Văn phòng làm việc", "office");

    private final String value;
    private final String type;

    RatingType(String value, String type) {
        this.value = value;
        this.type = type;
    }
}
