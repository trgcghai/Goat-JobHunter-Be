package iuh.fit.goat.dto.request.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogUpdateRequest {
    @NotNull(message = "Blog ID là bắt buộc")
    @Positive(message = "Blog ID phải là một số dương")
    private Long blogId;

    @Size(max = 2048, message = "URL banner không được vượt quá 2048 ký tự")
    private List<String> images;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private List<@NotBlank(message = "Tag không được để trống") String> tags;

}