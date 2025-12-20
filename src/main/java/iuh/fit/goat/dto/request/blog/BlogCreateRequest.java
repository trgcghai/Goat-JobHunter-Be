package iuh.fit.goat.dto.request.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogCreateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    private List<String> images;

    private String description;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private List<String> tags;

    @NotNull(message = "Trạng thái bản nháp phải được cung cấp")
    private Boolean draft;

}