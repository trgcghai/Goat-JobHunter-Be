package iuh.fit.goat.dto.request;

import iuh.fit.goat.util.annotation.DateRange;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DateRange
public class CreateJobRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 200, message = "Tiêu đề phải có ít nhất 10 ký tự và không vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String location;

    @NotNull(message = "Mức lương không được để trống")
    @DecimalMin(value = "1", inclusive = true, message = "Mức lương phải lớn hơn hoặc bằng 1")
    private Double salary;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, message = "Mô tả phải có ít nhất 50 ký tự")
    private String description;

    @NotBlank(message = "Cấp độ không được để trống")
    private String level;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    private Boolean active;

    @NotBlank(message = "Hình thức làm việc không được để trống")
    private String workingType;

    @NotNull(message = "Ngành nghề không được để trống")
    private Long careerId;

    @NotNull(message = "Phải chọn ít nhất 1 kỹ năng")
    @Size(min = 1, max = 10, message = "Chỉ được chọn từ 1 đến 10 kỹ năng")
    private List<@NotNull(message = "skillId không được để trống") Long> skillIds;

    @NotNull(message = "recruiterId không được để trống")
    @Positive(message = "recruiterId phải là số dương")
    private Long recruiterId;
}