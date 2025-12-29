package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum RatingType {

    OVERALL("Đánh giá chung", "overall", "Overall"),
    SALARY("Lương thưởng & phúc lợi", "salary", "Salary & Benefits"),
    TRAINING("Đào tạo & học hỏi", "training", "Training & Learning"),
    MANAGEMENT("Sự quan tâm đến nhân viên", "management", "Management Cares About Me"),
    CULTURE("Văn hóa công ty", "culture", "Culture & Fun"),
    OFFICE("Văn phòng làm việc", "office", "Office & Workspace");

    private final String value;
    private final String type;
    private final String original;

    RatingType(String value, String type, String original) {
        this.value = value;
        this.type = type;
        this.original = original;
    }
}
