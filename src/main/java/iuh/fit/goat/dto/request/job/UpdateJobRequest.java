package iuh.fit.goat.dto.request.job;

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
public class UpdateJobRequest {
    @NotNull(message = "jobId is required")
    private Long jobId;

    @Size(min = 10, max = 200, message = "Tiêu đề phải có ít nhất 10 ký tự và không vượt quá 200 ký tự")
    private String title;

    @Size(min = 1, max = 255, message = "Địa điểm phải có ít nhất 1 ký tự và không vượt quá 255 ký tự")
    private String location;

    @DecimalMin(value = "1", inclusive = true, message = "Mức lương phải lớn hơn hoặc bằng 1")
    private Double salary;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @Size(min = 50, message = "Mô tả phải có ít nhất 50 ký tự")
    private String description;

    @Size(min = 1, message = "Cấp độ không được để trống")
    private String level;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;

    @Size(min = 1, message = "Hình thức làm việc không được để trống")
    private String workingType;

    private Long careerId;

    @Size(min = 1, max = 10, message = "Chỉ được chọn từ 1 đến 10 kỹ năng")
    private List<@NotNull(message = "skillId không được để trống") Long> skillIds;
}